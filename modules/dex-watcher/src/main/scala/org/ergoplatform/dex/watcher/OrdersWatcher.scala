package org.ergoplatform.dex.watcher

import cats.{Functor, Monad}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import org.ergoplatform.dex.HexString
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.streaming.models.Output
import org.ergoplatform.dex.watcher.context._
import tofu.logging._
import tofu.syntax.logging._

trait OrdersWatcher[F[_]] {

  def run: Stream[F, Unit]
}

object OrdersWatcher {

  def apply[G[_]: Functor, F[_]: Monad: HasWatcherContext](
    implicit logs: Logs[G, F]
  ): G[OrdersWatcher[F]] =
    logs.forService[OrdersWatcher[F]].map(implicit l => new Live[F])

  final private class Live[F[_]: Monad: Logging: HasWatcherContext] extends OrdersWatcher[F] {

    def run: Stream[F, Unit] =
      Stream.force(
        askConsumer.map {
          _.consume { rec =>
            Stream.emit(rec).unNone.flatMap { tx =>
              process(tx.outputs) >> Stream.eval(debug"${tx.outputs.size} boxes processed")
            }
          }
        }
      )

    private def process(outputs: List[Output]): Stream[F, Unit] =
      Stream.force(
        askProducer.map { producer =>
          Stream
            .emit(outputs)
            .evalMap(makeOrders)
            .flatMap(Stream.emits)
            .through(producer.produce)
        }
      )

    private def makeOrders: List[Output] => F[List[AnyOrder]] =
      _.foldLeft(Vector.empty[AnyOrder].pure[F]) { (acc, out) =>
        if (isOrder(out.ergoTree)) acc >>= (xs => makeOrder(out).map(xs :+ _))
        else acc
      }.map(_.toList).flatTap(orders => info"${orders.size} orders extracted")

    private def makeOrder(out: Output): F[AnyOrder] = ???

    private def isOrder(ergoTree: HexString): Boolean = ???
  }
}
