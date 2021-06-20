package org.ergoplatform.dex.resolver.processes

import cats.{Foldable, Functor, Monad}
import org.ergoplatform.common.streaming.syntax._
import org.ergoplatform.common.streaming.Consumer
import org.ergoplatform.dex.domain.amm.state.OnChain
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.resolver.repositories.Pools
import tofu.streams.Evals
import tofu.syntax.streams.all._

trait PoolTracker[F[_]] {

  def run: F[Unit]
}

object PoolTracker {

  final class Live[
    F[_]: Monad: Evals[*[_], G],
    G[_]: Functor,
    C[_]: Foldable
  ](implicit
    consumer: Consumer[PoolId, OnChain[CFMMPool], F, G],
    pools: Pools[G]
  ) extends PoolTracker[F] {

    def run: F[Unit] =
      consumer.stream
        .evalTap(rec => pools.put(rec.message))
        .evalMap(_.commit)
  }
}
