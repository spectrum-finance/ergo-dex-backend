package org.ergoplatform.dex.markets.processes

import cats.{Functor, Monad}
import cats.effect.Timer
import cats.effect.concurrent.Ref
import org.ergoplatform.dex.markets.services.FiatRates.ErgUsdPoolNft
import org.ergoplatform.dex.protocol.constants.ErgoAssetDecimals
import org.ergoplatform.ergo.domain.{RegisterId, SConstant}
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import tofu.Catches
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.monadic.unit
import tofu.syntax.streams.evals.eval
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.handle._
import cats.syntax.option._

import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait RatesProcess[S[_]] {
  def run: S[Unit]
}

object RatesProcess {

  val MemoTtl: FiniteDuration = 2.minutes

  def make[I[_]: Functor, F[_]: Monad: Timer, S[_]: Monad: Evals[*[_], F]: Catches](implicit
    network: ErgoExplorer[F],
    cache: Ref[F, Option[BigDecimal]],
    logs: Logs[I, F]
  ): I[RatesProcess[S]] =
    logs.forService[Impl[S, F]].map(implicit __ => new Impl[S, F])

  final private class Impl[S[_]: Monad: Evals[*[_], F]: Catches, F[_]: Monad: Logging: Timer](implicit
    network: ErgoExplorer[F],
    cache: Ref[F, Option[BigDecimal]]
  ) extends RatesProcess[S] {

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
  }
}
