package org.ergoplatform.dex.tracker.processes

import cats.effect.concurrent.Ref
import cats.syntax.option._
import cats.{Defer, FlatMap, FunctorFilter, Monad, MonoidK}
import derevo.derive
import mouse.any._
import org.ergoplatform.dex.OrderId
import org.ergoplatform.dex.clients.StreamingErgoNetworkClient
import org.ergoplatform.dex.clients.explorer.models.Output
import org.ergoplatform.dex.domain.orderbook.Order.AnyOrder
import org.ergoplatform.dex.streaming.{Producer, Record}
import org.ergoplatform.dex.tracker.configs.TrackerConfig
import org.ergoplatform.dex.tracker.domain.errors.InvalidOrder
import org.ergoplatform.dex.tracker.modules.Orders
import tofu.concurrent.MakeRef
import tofu.higherKind.derived.representableK
import tofu.logging._
import tofu.streams.{Evals, Pace}
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.handle._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._
import tofu.{Catches, Handle}

import scala.concurrent.duration._

@derive(representableK)
trait OrdersTracker[F[_]] {

  def run: F[Unit]
}

object OrdersTracker {

  private val retryDelay = 5.seconds

  def make[
    I[_]: FlatMap,
    F[_]: Monad: Evals[*[_], G]: Pace: FunctorFilter: Defer: MonoidK: TrackerConfig.Has: Catches,
    G[_]: Monad: Handle[*[_], InvalidOrder]
  ](implicit
    client: StreamingErgoNetworkClient[F, G],
    producer: Producer[OrderId, AnyOrder, F],
    logs: Logs[I, G],
    orders: Orders[G],
    makeRef: MakeRef[I, G]
  ): I[OrdersTracker[F]] =
    logs.forService[OrdersTracker[F]].flatMap { implicit l =>
      makeRef.refOf(0).map { ref =>
        (context map (conf => new Live[F, G](ref, conf): OrdersTracker[F])).embed
      }
    }

  final private class Live[
    F[_]: Monad: Evals[*[_], G]: Pace: FunctorFilter: Defer: MonoidK: Catches,
    G[_]: Monad: Handle[*[_], InvalidOrder]: Logging
  ](lastScannedHeightRef: Ref[G, Int], conf: TrackerConfig)(implicit
    client: StreamingErgoNetworkClient[F, G],
    producer: Producer[OrderId, AnyOrder, F],
    orders: Orders[G]
  ) extends OrdersTracker[F] {

    def run: F[Unit] =
      eval(client.getCurrentHeight).repeat
        .throttled(conf.scanInterval)
        .flatMap { height =>
          eval(lastScannedHeightRef.get)
            .evalTap { lastScannedHeight =>
              if (lastScannedHeight < height) info"Checking height delta ($lastScannedHeight, $height)"
              else info"Waiting for new blocks. Current height is [$height]"
            }
            .flatMap { lastScannedHeight =>
              val lastEpochs = (height - lastScannedHeight) min conf.scanLastEpochs
              if (lastEpochs > 0) client.streamUnspentOutputs(lastEpochs)
              else MonoidK[F].empty[Output]
            }
            .flatTap(_ => eval(lastScannedHeightRef.set(height)))
        }
        .evalTap(out => trace"Processing box $out")
        .thrush(process)
        .handleWith[Throwable] { e =>
          eval(warnCause"Tracker failed. Retrying in ${retryDelay.toMillis} ms"(e)) >> run
        }

    private def process: F[Output] => F[Unit] =
      _.evalMap { out =>
        orders
          .makeOrder(out)
          .handleWith[InvalidOrder] { e =>
            warnCause"Skipping invalid order in box $out" (e) as none[AnyOrder]
          }
      }.unNone
        .evalTap(order => info"Order detected $order")
        .map(o => Record[OrderId, AnyOrder](o.id, o))
        .thrush(producer.produce)
  }
}
