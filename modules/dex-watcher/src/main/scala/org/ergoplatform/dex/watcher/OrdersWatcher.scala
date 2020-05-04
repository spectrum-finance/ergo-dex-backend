package org.ergoplatform.dex.watcher

import cats.effect.Concurrent
import fs2.concurrent.Queue
import fs2.{Pipe, Stream}
import org.ergoplatform.dex.HexString
import org.ergoplatform.dex.watcher.domain.Order.{BuyOrder, SellOrder}
import org.ergoplatform.dex.watcher.domain.{ErgoBox, Order}
import org.ergoplatform.dex.watcher.streaming.Consumer

abstract class OrdersWatcher[F[_], S[_[_] <: F[_], _]] {

  def run: S[F, Unit]
}

object OrdersWatcher {

  final private class Live[F[_]: Concurrent](consumer: Consumer[F, Stream], q: Queue[F, Order[_]])
    extends OrdersWatcher[F, Stream] {

    def run: Stream[F, Unit] =
      consumer.stream.through(process)

    private def process: Pipe[F, ErgoBox, Unit] =
      _.broadcastThrough(
        (_: Stream[F, ErgoBox]).filter(box => isSellOrder(box.ergoTree)).evalMap(makeSellOrder),
        (_: Stream[F, ErgoBox]).filter(box => isBuyOrder(box.ergoTree)).evalMap(makeBuyOrder)
      ).through(q.enqueue)

    private def makeSellOrder(box: ErgoBox): F[SellOrder] = ???
    private def makeBuyOrder(box: ErgoBox): F[BuyOrder]   = ???

    private def isSellOrder(ergoTree: HexString): Boolean = ???
    private def isBuyOrder(ergoTree: HexString): Boolean  = ???
  }
}
