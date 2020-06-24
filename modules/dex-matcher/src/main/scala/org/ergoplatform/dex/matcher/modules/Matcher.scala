package org.ergoplatform.dex.matcher.modules

import cats.{Functor, Monad, Parallel}
import fs2._
import cats.instances.list._
import cats.syntax.parallel._
import org.ergoplatform.dex.matcher.configs.MatcherConfig
import org.ergoplatform.dex.matcher.services.OrderBook
import org.ergoplatform.dex.domain.syntax.order._
import org.ergoplatform.dex.matcher.streaming.StreamingBundle
import tofu.HasContext
import tofu.syntax.context._
import tofu.syntax.monadic._
import tofu.fs2Instances._
import tofu.logging.{Logging, Logs}

trait Matcher[F[_]] {

  def run: Stream[F, Unit]
}

object Matcher {

  def make[
    I[_]: Functor,
    F[_]: Monad: Parallel: HasContext[*[_], MatcherConfig]
  ](
    streaming: StreamingBundle[F],
    orderBook: OrderBook[F]
  )(implicit logs: Logs[I, F]): I[Matcher[F]] =
    logs.forService[Matcher[F]].map { implicit l =>
      new Live[F](streaming, orderBook)
    }

  final private class Live[F[_]: Monad: Parallel: Logging: HasContext[*[_], MatcherConfig]](
    streaming: StreamingBundle[F],
    orderBook: OrderBook[F]
  ) extends Matcher[F] {

    def run: Stream[F, Unit] =
      context[Stream[F, *]] >>= { conf =>
        streaming.consumer.consumeBatch(conf.batchSize, conf.interval) { ch =>
          val matches = ch
            .collect { case Some(order) => order }
            .toList
            .groupBy(_.pairId)
            .toList
            .parFlatTraverse {
              case (pairId, orders) =>
                orderBook.process(pairId)(orders)
            }
          Stream.evals(matches).through(streaming.producer.produce)
        }
      }
  }
}
