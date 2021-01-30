package org.ergoplatform.dex.matcher.processes

import cats.instances.list._
import cats.syntax.foldable._
import cats.{Foldable, Functor, Monad}
import derevo.derive
import mouse.any._
import org.ergoplatform.dex.TradeId
import org.ergoplatform.dex.context.HasCommitPolicy
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.matcher.configs.MatcherConfig
import org.ergoplatform.dex.matcher.services.OrderBook
import org.ergoplatform.dex.matcher.streaming.StreamingBundle
import org.ergoplatform.dex.streaming.{CommitPolicy, Record}
import org.ergoplatform.dex.streaming.syntax._
import tofu.HasContext
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, ParFlatten, Temporal}
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.streams.emits._
import tofu.syntax.streams.evals._
import tofu.syntax.streams.parFlatten._
import tofu.syntax.streams.temporal._

@derive(representableK)
trait Matcher[F[_]] {

  def run: F[Unit]
}

object Matcher {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: Temporal[*[_], C]: ParFlatten: HasContext[*[_], MatcherConfig]: HasCommitPolicy,
    G[_]: Monad,
    C[_]: Foldable
  ](implicit
    streaming: StreamingBundle[F, G],
    orderBook: OrderBook[G],
    logs: Logs[I, G]
  ): I[Matcher[F]] =
    logs.forService[Matcher[F]].map { implicit l =>
      (hasContext[F, MatcherConfig], hasContext[F, CommitPolicy])
        .mapN((conf, policy) => new Live[F, G, C](conf, policy): Matcher[F])
        .embed
    }

  final private class Live[
    F[_]: Monad: Evals[*[_], G]: Temporal[*[_], C]: ParFlatten,
    G[_]: Monad: Logging,
    C[_]: Foldable
  ](config: MatcherConfig, commitPolicy: CommitPolicy)(implicit
    streaming: StreamingBundle[F, G],
    orderBook: OrderBook[G]
  ) extends Matcher[F] {

    def run: F[Unit] =
      streaming.consumer.stream
        .groupWithin(config.batchSize, config.batchInterval)
        .flatTap { batch =>
          val pairs = batch.toList.map(_.message).groupBy(_.pairId).toList
          emits(pairs.map { case (pairId, orders) => evals(orderBook.process(pairId)(orders)) }).parFlattenUnbounded
            .map(t => Record[TradeId, AnyTrade](t.id, t))
            .thrush(streaming.producer.produce)
        }
        .flatMap(emits(_))
        .commitBatchWithin[C](commitPolicy.maxBatchSize, commitPolicy.commitTimeout)
  }
}
