package org.ergoplatform.dex.watcher

import cats.{Functor, Monad}
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import fs2.Stream
import org.ergoplatform.dex.HexString
import org.ergoplatform.dex.domain.Order
import org.ergoplatform.dex.domain.models.Output
import org.ergoplatform.dex.watcher.context._
import tofu.logging._
import tofu.syntax.logging._

abstract class OrdersWatcher[F[_], S[_[_] <: F[_], _]] {

  def run: S[F, Unit]
}

object OrdersWatcher {

  def apply[G[_]: Functor, F[_]: Monad: HasWatcherContext](
    implicit logs: Logs[G, F]
  ): G[OrdersWatcher[F, Stream]] =
    logs.forService[OrdersWatcher[F, Stream]].map(implicit l => new Live[F])

  final private class Live[F[_]: Monad: Logging: HasWatcherContext]
    extends OrdersWatcher[F, Stream] {

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

    private def makeOrders: List[Output] => F[List[Order]] =
      _.foldLeft(Array.empty[Order].pure[F]) { (acc, out) =>
        if (isOrder(out.ergoTree)) acc >>= (xs => makeOrder(out).map(xs :+ _))
        else acc
      }.map(_.toList).flatTap(orders => info"${orders.size} orders extracted")

    private def makeOrder(out: Output): F[Order] = ???

    private def isOrder(ergoTree: HexString): Boolean = ???
  }
}
