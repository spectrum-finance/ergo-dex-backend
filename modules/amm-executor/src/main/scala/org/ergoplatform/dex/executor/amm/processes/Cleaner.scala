package org.ergoplatform.dex.executor.amm.processes

import cats.{Functor, Monad}
import derevo.derive
import org.ergoplatform.dex.executor.amm.modules.CFMMBacklog
import org.ergoplatform.dex.executor.amm.streaming.EvaluatedCFMMOrders
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

@derive(representableK)
trait Cleaner[F[_]] {

  def run: F[Unit]
}

object Cleaner {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G],
    G[_]: Monad
  ](implicit
    orders: EvaluatedCFMMOrders[F, G],
    backlog: CFMMBacklog[G],
    logs: Logs[I, G]
  ): I[Cleaner[F]] =
    logs.forService[Cleaner[F]].map(implicit l => new Live[F, G])

  final private class Live[
    F[_]: Monad: Evals[*[_], G],
    G[_]: Monad: Logging
  ](implicit
    orders: EvaluatedCFMMOrders[F, G],
    backlog: CFMMBacklog[G]
  ) extends Cleaner[F] {

    def run: F[Unit] =
      orders.stream
        .evalTap(rec => backlog.drop(rec.message.order.id).ifM(debug"Order ${rec.message} is evicted", unit[G]))
        .evalMap(_.commit)
  }
}
