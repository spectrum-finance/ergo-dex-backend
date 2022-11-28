package org.ergoplatform.dex.executor.amm.processes

import cats.Monad
import cats.effect.concurrent.Ref
import cats.effect.{Sync, Timer}
import org.ergoplatform.dex.domain.NetworkContext
import org.ergoplatform.ergo.domain.EpochParams
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

import scala.concurrent.duration.DurationInt

trait NetworkContextUpdater[S[_]] {
  def run: S[Unit]
}

object NetworkContextUpdater {

  def make[I[_]: Sync, S[_]: Evals[*[_], F]: Monad, F[_]: Sync: Timer](implicit
    network: ErgoExplorer[F],
    logs: Logs[I, F]
  ): I[(NetworkContextUpdater[S], Ref[F, NetworkContext])] =
    Ref.in[I, F, NetworkContext](NetworkContext(0, EpochParams.empty)).flatMap { ref =>
      logs.forService[NetworkContextUpdater[S]].map(implicit __ => new Live[S, F](ref) -> ref)
    }

  final private class Live[S[_]: Evals[*[_], F]: Monad, F[_]: Monad: Timer: Logging](context: Ref[F, NetworkContext])(
    implicit network: ErgoExplorer[F]
  ) extends NetworkContextUpdater[S] {

    def run: S[Unit] =
      eval(for {
        height <- network.getCurrentHeight
        params <- network.getEpochParams
        _      <- info"New height is: $height"
        _      <- context.set(NetworkContext(height, params))
        _      <- Timer[F].sleep(3.minutes)
      } yield ()) >> run

  }
}
