package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.{Functor, Monad}
import org.ergoplatform.dex.index.db.models.LiquidityProviderSnapshot
import org.ergoplatform.dex.index.repositories.LiquidityProvidersRepo
import tofu.streams.{Chunks, Evals}
import tofu.syntax.monadic._
import tofu.syntax.logging._
import cats.syntax.traverse._
import tofu.logging.{Logging, Logs}

trait StatesResolver[F[_]] {
  def resolve: F[Unit]
}

object StatesResolver {

  def make[I[_]: Functor, F[_]: Monad](implicit
    repo: LiquidityProvidersRepo[F],
    logs: Logs[I, F]
  ): I[StatesResolver[F]] =
    logs.forService[StatesResolver[F]].map(implicit __ => new Live[F](repo))

  final class Live[F[_]: Monad: Logging](repo: LiquidityProvidersRepo[F]) extends StatesResolver[F] {

    val fixedTimestamp: Long         = 1666396799000L
    val fixedTimestampToInsert: Long = 1666396799001L

    def resolve: F[Unit] = {
      info"Start resolving" >>
      repo.getAllUnresolvedStates
      .flatMap { states =>
        states
          .map { state =>
            val gap    = fixedTimestamp - state.timestamp
            val weight = gap * state.lpErg
            val resolvedState =
              LiquidityProviderSnapshot.resolved(
                state.address,
                state.poolId,
                state.lpId,
                fixedTimestampToInsert,
                weight,
                gap
              )
            info"Going to resolve state $state as $resolvedState" >> repo.insert(NonEmptyList.one(resolvedState))
          }
          .sequence
          .void
      } <* info"All states resolved"
    }
  }
}
