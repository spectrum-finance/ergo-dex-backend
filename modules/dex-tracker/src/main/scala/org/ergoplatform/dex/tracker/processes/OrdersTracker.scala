package org.ergoplatform.dex.tracker.processes

import cats.effect.concurrent.Ref
import cats.{FlatMap, Foldable, FunctorFilter, Monad, MonoidK}
import derevo.derive
import mouse.any._
import org.ergoplatform.dex.OrderId
import org.ergoplatform.dex.clients.StreamingErgoNetworkClient
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.explorer.models.Output
import org.ergoplatform.dex.streaming.{Producer, Record}
import org.ergoplatform.dex.tracker.modules.Orders
import tofu.concurrent.MakeRef
import tofu.higherKind.derived.representableK
import tofu.logging._
import tofu.streams.{Evals, Temporal}
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.streams.evals._
import tofu.syntax.streams.filter._

@derive(representableK)
trait OrdersTracker[F[_]] {

  def run: F[Unit]
}

object OrdersTracker {

  def make[
    I[_]: FlatMap,
    F[_]: Monad: Evals[*[_], G]: Temporal[*[_], C]: FunctorFilter: MonoidK,
    G[_]: Monad,
    C[_]: Foldable
  ](implicit
    client: StreamingErgoNetworkClient[F, G],
    producer: Producer[OrderId, AnyOrder, F],
    logs: Logs[I, G],
    orders: Orders[G],
    makeRef: MakeRef[I, G]
  ): I[OrdersTracker[F]] =
    logs.forService[OrdersTracker[F]].flatMap { implicit l =>
      makeRef.refOf(0).map(new Live[F, G, C](_): OrdersTracker[F])
    }

  final private class Live[
    F[_]: Monad: Evals[*[_], G]: Temporal[*[_], C]: FunctorFilter: MonoidK,
    G[_]: Monad: Logging,
    C[_]: Foldable
  ](lastScannedHeightRef: Ref[G, Int])(implicit
    client: StreamingErgoNetworkClient[F, G],
    producer: Producer[OrderId, AnyOrder, F],
    orders: Orders[G]
  ) extends OrdersTracker[F] {

    private val MaxEpochs = 100

    def run: F[Unit] =
      eval(client.getCurrentHeight)
        .flatMap { height =>
          eval(lastScannedHeightRef.get)
            .flatMap { lastScannedHeight =>
              val lastEpochs = (height - lastScannedHeight) min MaxEpochs
              if (lastEpochs > 0) client.streamUnspentOutputs(lastEpochs)
              else MonoidK[F].empty[Output]
            }
            .flatTap(_ => eval(lastScannedHeightRef.set(height)))
        }
        .thrush(process)
        .void

    private def process: F[Output] => F[Unit] =
      _.evalMap(orders.makeOrder).unNone
        .map(o => Record[OrderId, AnyOrder](o.id, o))
        .thrush(producer.produce)
  }
}
