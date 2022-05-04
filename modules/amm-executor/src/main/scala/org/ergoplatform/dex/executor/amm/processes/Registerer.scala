package org.ergoplatform.dex.executor.amm.processes

import cats.{Functor, Monad}
import derevo.derive
import org.ergoplatform.common.TraceId
import org.ergoplatform.dex.executor.amm.modules.CFMMBacklog
import org.ergoplatform.dex.executor.amm.streaming.CFMMOrders
import tofu.Catches
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.handle._
import tofu.syntax.streams.all._

@derive(representableK)
trait Registerer[F[_]] {

  def run: F[Unit]
}

object Registerer {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: Catches,
    G[_]: Monad: TraceId.Local
  ](implicit
    orders: CFMMOrders[F, G],
    backlog: CFMMBacklog[G],
    logs: Logs[I, G]
  ): I[Registerer[F]] =
    logs.forService[Registerer[F]].map(implicit l => new Live[F, G])

  final private class Live[
    F[_]: Monad: Evals[*[_], G]: Catches,
    G[_]: Monad: Logging: TraceId.Local
  ](implicit
    orders: CFMMOrders[F, G],
    backlog: CFMMBacklog[G]
  ) extends Registerer[F] {

    def run: F[Unit] =
      orders.stream
        .evalTap(rec => debug"Registered ${rec.message}" >> backlog.put(rec.message))
        .evalMap(_.commit)
        .handleWith[Throwable](e => eval(warnCause"Registerer failed. Restarting .." (e)) >> run)
  }
}
