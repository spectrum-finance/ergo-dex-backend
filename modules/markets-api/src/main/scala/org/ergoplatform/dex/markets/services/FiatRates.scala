package org.ergoplatform.dex.markets.services

import cats.effect.{Clock, Sync, Timer}
import cats.effect.concurrent.Ref
import cats.{FlatMap, Functor, Monad}
import derevo.derive
import org.ergoplatform.common.caching.Memoize
import org.ergoplatform.dex.domain.{AssetClass, FiatUnits}
import org.ergoplatform.dex.markets.currencies.UsdUnits
import org.ergoplatform.dex.protocol.constants.{ErgoAssetClass, ErgoAssetDecimals}
import org.ergoplatform.ergo.domain.{RegisterId, SConstant}
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import tofu.concurrent.MakeRef
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.foption._
import tofu.syntax.logging._
import tofu.syntax.monadic._

import scala.concurrent.duration._

trait FiatRates[F[_]] {

  def rateOf(asset: AssetClass, units: FiatUnits): F[Option[BigDecimal]]

  def run: fs2.Stream[F, Unit]
}

object FiatRates {

  val ErgUsdPoolNft: TokenId =
    TokenId.fromStringUnsafe("011d3364de07e5a26f0c4eef0852cddb387039a921b7154ef3cab22c6eda887f")

  val MemoTtl: FiniteDuration = 2.minutes

  def make[I[_]: FlatMap: Sync, F[_]: Monad: Clock: Sync: Timer](implicit
    network: ErgoExplorer[F],
    logs: Logs[I, F],
    makeRef: MakeRef[I, F]
  ): I[FiatRates[F]] =
    for {
      implicit0(l: Logging[F]) <- logs.forService[FiatRates[F]]
      memo                     <- Memoize.make[I, F, BigDecimal]
      ref                      <- Ref.in[I, F, BigDecimal](BigDecimal(0))
      resolver = new ErgoOraclesRateSource(network, memo, ref)
    } yield resolver

  final class ErgoOraclesRateSource[F[_]: Monad: Logging: Timer](
    network: ErgoExplorer[F],
    memo: Memoize[F, BigDecimal],
    ref: Ref[F, BigDecimal]
  ) extends FiatRates[F] {

    def run: fs2.Stream[F, Unit] = {
      val pullFromNetwork = {
        info"Going to pull from network rate" >>
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
      }.flatTap(_ => info"rate from network finished")

      fs2.Stream.eval(pullFromNetwork).flatMap {
        case Some(rate) =>
          fs2.Stream.eval(memo.memoize(rate, 356.days)) >> fs2.Stream.eval(Timer[F].sleep(1.minutes))
        case None => fs2.Stream.eval(noneF)
      } >> run
    }

    def rateOf(asset: AssetClass, units: FiatUnits): F[Option[BigDecimal]] = {
      def f: F[Option[BigDecimal]] =
        if (asset == ErgoAssetClass && units == UsdUnits) {
          for {
            _            <- info"Memo read $asset $units"
            memoizedRate: Option[BigDecimal] <- memo.read
            _            <- info"Memo read completed $memoizedRate"
            res <- memoizedRate match {
                     case Some(rate) => rate.someF
                     case None       => noneF
                   }
          } yield res
        } else noneF
      for {
        _  <- info"rateOf $asset $units"
        f1 <- f
        _  <- info"rateOf finished $asset $units"
      } yield f1
    }
  }
}
