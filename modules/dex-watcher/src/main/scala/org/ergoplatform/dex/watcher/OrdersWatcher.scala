package org.ergoplatform.dex.watcher

import cats.Applicative
import cats.syntax.applicative._
import fs2.Stream
import org.ergoplatform.dex.HexString
import org.ergoplatform.dex.domain.ErgoBox
import org.ergoplatform.dex.domain.Order.{BuyOrder, SellOrder}
import org.ergoplatform.dex.domain.models.{Output, Transaction}
import org.ergoplatform.dex.streaming.Consumer

abstract class OrdersWatcher[F[_], S[_[_] <: F[_], _]] {

  def run: S[F, Unit]
}

object OrdersWatcher {

  final private class Live[F[_]: Applicative](consumer: Consumer[F, Stream, Transaction])
    extends OrdersWatcher[F, Stream] {

    def run: Stream[F, Unit] =
      consumer.processStream(4) {
        case Some(tx) => process(tx.outputs)
        case None     => ().pure[F]
      }

    private def process(outputs: List[Output]): F[Unit] = ???

    private def makeSellOrder(box: ErgoBox): F[SellOrder] = ???
    private def makeBuyOrder(box: ErgoBox): F[BuyOrder]   = ???

    private def isSellOrder(ergoTree: HexString): Boolean = ???
    private def isBuyOrder(ergoTree: HexString): Boolean  = ???
  }
}
