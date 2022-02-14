package org.ergoplatform.dex.resolver.processes

import cats.{FlatMap, Monad}
import org.ergoplatform.common.streaming.Consumer
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.resolver.repositories.CFMMPools
import org.ergoplatform.ergo.state.{ConfirmedIndexed, Unconfirmed}
import tofu.streams.{Evals, ParFlatten}
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

trait PoolTracker[F[_]] {

  def run: F[Unit]
}

object PoolTracker {

  def make[
    F[_]: Monad: Evals[*[_], G]: ParFlatten,
    G[_]: FlatMap
  ](implicit
    confirmedPools: Consumer[PoolId, ConfirmedIndexed[CFMMPool], F, G],
    unconfirmedPools: Consumer[PoolId, Unconfirmed[CFMMPool], F, G],
    pools: CFMMPools[G]
  ): PoolTracker[F] = new Live[F, G]

  final class Live[
    F[_]: Monad: Evals[*[_], G]: ParFlatten,
    G[_]: FlatMap
  ](implicit
    confirmedPools: Consumer[PoolId, ConfirmedIndexed[CFMMPool], F, G],
    unconfirmedPools: Consumer[PoolId, Unconfirmed[CFMMPool], F, G],
    pools: CFMMPools[G]
  ) extends PoolTracker[F] {

    def run: F[Unit] =
      emits(
        List(
          confirmedPools.stream.evalMap(rec => pools.put(rec.message) >> rec.commit),
          unconfirmedPools.stream.evalMap(rec => pools.put(rec.message) >> rec.commit)
        )
      ).parFlattenUnbounded
  }
}
