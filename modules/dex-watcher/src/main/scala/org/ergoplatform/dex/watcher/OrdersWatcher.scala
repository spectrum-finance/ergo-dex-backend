package org.ergoplatform.dex.watcher

import cats.Applicative
import cats.syntax.functor._
import fs2.Stream
import org.ergoplatform.dex.HexString
import org.ergoplatform.dex.domain.Order.{AnyOrder, BuyOrder, SellOrder}
import org.ergoplatform.dex.domain.models.Output
import org.ergoplatform.dex.watcher.context.WatcherContext
import tofu.HasContext
import tofu.syntax.context.{context => ctx}

abstract class OrdersWatcher[F[_], S[_[_] <: F[_], _]] {

  def run: S[F, Unit]
}

object OrdersWatcher {

  final private class Live[F[_]: Applicative](
    implicit F: F HasContext WatcherContext[F]
  ) extends OrdersWatcher[F, Stream] {

    def run: Stream[F, Unit] =
      Stream.force(
        ctx[F].map(_.consumer).map {
          _.consume { rec =>
            Stream.emit(rec).unNone.flatMap(tx => process(tx.outputs))
          }
        }
      )

    private def process(outputs: List[Output]): Stream[F, Unit] =
      Stream.force(
        ctx[F].map(_.producer).map { producer =>
          Stream
            .emit(outputs)
            .map { makeOrders(_).map("commonKey" -> _) } // todo:
            .flatMap(Stream.emits)
            .through(producer.produce)
        }
      )

    private def makeOrders: List[Output] => List[AnyOrder] = ???

    private def makeSellOrder(out: Output): F[SellOrder] = ???
    private def makeBuyOrder(out: Output): F[BuyOrder]   = ???

    private def isSellOrder(ergoTree: HexString): Boolean = ???
    private def isBuyOrder(ergoTree: HexString): Boolean  = ???
  }
}
