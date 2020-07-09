package org.ergoplatform.dex.watcher.modules

import cats.{Functor, Monad}
import fs2.Stream
import org.ergoplatform.dex.HexString
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.streaming.models.Output
import org.ergoplatform.dex.watcher.streaming.StreamingBundle
import tofu.logging._
import tofu.syntax.logging._
import tofu.syntax.monadic._

trait OrdersWatcher[F[_]] {

  def run: Stream[F, Unit]
}

object OrdersWatcher {

  def make[I[_]: Functor, F[_]: Monad](streaming: StreamingBundle[F])(
    implicit logs: Logs[I, F]
  ): I[OrdersWatcher[F]] =
    logs.forService[OrdersWatcher[F]].map(implicit l => new Live[F](streaming))

  final private class Live[F[_]: Monad: Logging](streaming: StreamingBundle[F])
    extends OrdersWatcher[F] {

    import Live._

    def run: Stream[F, Unit] =
      streaming.consumer.consume { rec =>
        Stream.emit(rec).unNone.flatMap { tx =>
          process(tx.outputs) >> Stream.eval(debug"${tx.outputs.size} boxes processed")
        }
      }

    private def process(outputs: List[Output]): Stream[F, Unit] =
      Stream
        .emit(outputs)
        .evalMap(makeOrders)
        .flatMap(Stream.emits)
        .through(streaming.producer.produce)
  }

  private[watcher] object Live {

    def makeOrders[F[_]: Monad: Logging]: List[Output] => F[List[AnyOrder]] =
      _.foldLeft(Vector.empty[AnyOrder].pure[F]) { (acc, out) =>
        if (isOrder(out.ergoTree) && isValid(out)) acc >>= (xs => makeOrder[F](out).map(xs :+ _))
        else acc
      }.map(_.toList).flatTap(orders => info"${orders.size} orders extracted")

    def makeOrder[F[_]](out: Output): F[AnyOrder] = ???

    def isOrder(ergoTree: HexString): Boolean = ???

    def isValid(out: Output): Boolean = ???
  }
}
