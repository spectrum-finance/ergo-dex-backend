package org.ergoplatform.dex.markets.repositories

import cats.tagless.FunctorK
import cats.tagless.syntax.functorK._
import cats.{~>, FlatMap, Monad}
import tofu.doobie.LiftConnectionIO
import tofu.doobie.log.EmbeddableLogHandler
import tofu.logging.Logs
import tofu.syntax.monadic._

final case class RepoBundle[F[_]](trades: TradesRepo[F])

object RepoBundle {

  implicit def functorK: FunctorK[RepoBundle] =
    new FunctorK[RepoBundle] {

      def mapK[F[_], G[_]](af: RepoBundle[F])(fk: F ~> G): RepoBundle[G] =
        RepoBundle(af.trades.mapK(fk))
    }

  def make[I[_]: Monad, F[_]: FlatMap: LiftConnectionIO](implicit
    elh: EmbeddableLogHandler[F],
    logs: Logs[I, F]
  ): I[RepoBundle[F]] =
    for {
      trades <- TradesRepo.make[I, F]
    } yield RepoBundle(trades)
}
