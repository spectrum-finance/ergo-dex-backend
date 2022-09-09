package org.ergoplatform.dex.markets.services

import cats.effect.concurrent.Ref
import cats.effect.{Clock, Sync, Timer}
import cats.syntax.option._
import cats.{FlatMap, Monad}
import org.ergoplatform.dex.domain.{AssetClass, FiatUnits}
import org.ergoplatform.dex.markets.currencies.UsdUnits
import org.ergoplatform.dex.protocol.constants.{ErgoAssetClass, ErgoAssetDecimals}
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.ergo.domain.{RegisterId, SConstant}
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import tofu.Catches
import tofu.concurrent.MakeRef
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.foption._
import tofu.syntax.handle._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.evals._

import scala.concurrent.duration._

trait FiatRates[F[_], S[_]] {

  def rateOf(asset: AssetClass, units: FiatUnits): F[Option[BigDecimal]]

  def run: S[Unit]
}

object FiatRates {

  val ErgUsdPoolNft: TokenId =
    TokenId.fromStringUnsafe("011d3364de07e5a26f0c4eef0852cddb387039a921b7154ef3cab22c6eda887f")

  val MemoTtl: FiniteDuration = 2.minutes

  def make[I[_]: FlatMap: Sync, S[_]: Monad: Evals[*[_], F]: Catches, F[_]: Monad: Clock: Sync: Timer](implicit
    network: ErgoExplorer[F],
    logs: Logs[I, F],
    makeRef: MakeRef[I, F]
  ): I[FiatRates[F, S]] =
    for {
      implicit0(l: Logging[F]) <- logs.forService[FiatRates[F, S]]
      cache                    <- Ref.in[I, F, Option[BigDecimal]](none)
      resolver = new ErgoOraclesRateSource[S, F](network, cache)
    } yield resolver

  final class ErgoOraclesRateSource[S[_]: Monad: Evals[*[_], F]: Catches, F[_]: Monad: Logging: Timer](
    network: ErgoExplorer[F],
    cache: Ref[F, Option[BigDecimal]]
  ) extends FiatRates[F, S] {

    def run: S[Unit] = {
      val pullFromNetwork =
        info"Going to pull rate from network" >>
        network
          .getUtxoByToken(ErgUsdPoolNft, offset = 0, limit = 1)
          .map(_.headOption)
          .map {
            for {
              out    <- _
              (_, r) <- out.additionalRegisters.find { case (r, _) => r == RegisterId.R4 }
              usdPrice <- r match {
                            case SConstant.LongConstant(v) => Some(v)
                            case _                         => None
                          }
              oneErg = math.pow(10, ErgoAssetDecimals.toDouble)
            } yield BigDecimal(oneErg) / BigDecimal(usdPrice)
          }
          .flatTap(_ => info"Pull rate from network finished")

      eval(pullFromNetwork).flatMap {
        case Some(rate) =>
          eval(cache.set(rate.some)) >> eval(info"Going to sleep $MemoTtl" >> Timer[F].sleep(MemoTtl))
        case None => eval(unit[F])
      } >> run
    }
      .handleWith { err: Throwable =>
        eval(info"The error: ${err.getMessage} occurred.") >> run
      }

    def rateOf(asset: AssetClass, units: FiatUnits): F[Option[BigDecimal]] =
      if (asset == ErgoAssetClass && units == UsdUnits) {
        for {
          _                                <- trace"Memo read $asset $units"
          memoizedRate: Option[BigDecimal] <- cache.get
          _                                <- trace"Memo read completed $memoizedRate"
        } yield memoizedRate
      } else noneF[F, BigDecimal]
  }
}
