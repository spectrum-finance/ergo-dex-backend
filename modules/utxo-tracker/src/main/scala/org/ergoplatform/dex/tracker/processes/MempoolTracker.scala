package org.ergoplatform.dex.tracker.processes

import cats.effect.Clock
import cats.effect.concurrent.Ref
import cats.{Defer, Monad, MonoidK}
import org.ergoplatform.dex.tracker.configs.MempoolTrackingConfig
import org.ergoplatform.dex.tracker.handlers.BoxHandler
import org.ergoplatform.dex.tracker.processes.MempoolTracker.Filter
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.modules.MempoolStreaming
import tofu.Catches
import tofu.logging.Logging
import tofu.streams.{Evals, Pace, ParFlatten}
import tofu.syntax.streams.all._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.time._

import scala.annotation.tailrec

final class MempoolTracker[
  F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: Catches,
  G[_]: Monad: Logging
](conf: MempoolTrackingConfig, filter: Filter[G], handlers: BoxHandler[F]*)(implicit mempool: MempoolStreaming[F])
  extends UtxoTracker[F] {

  def run: F[Unit] =
    for {
      _       <- eval(info"Starting Mempool Tracker ..")
      output  <- mempool.streamUnspentOutputs
      unknown <- eval(filter.probe(output.boxId))
      _ <- if (unknown) emits(handlers.map(_(output.pure[F]))).parFlattenUnbounded
           else unit[F]
      _ <- run.delay(conf.samplingInterval)
    } yield ()
}

object MempoolTracker {

  final case class Bucket(epochStart: Long, members: Set[BoxId])

  final class Filter[F[_]: Monad: Clock](epochLength: Long, maxBuckets: Int, filter: Ref[F, List[Bucket]]) {

    def probe(box: BoxId): F[Boolean] = {
      @tailrec
      def probe0(buckets: List[Bucket]): Boolean =
        buckets match {
          case Nil        => false
          case head :: tl => if (head.members.contains(box)) true else probe0(tl)
        }
      for {
        buckets <- filter.get
        exists = probe0(buckets)
        ts <- now.millis
        _ <- if (exists) unit
             else
               buckets.headOption match {
                 case Some(bucket @ Bucket(epochStart, members)) if ts - epochStart < epochLength =>
                   filter.set(bucket.copy(members = members + box) :: buckets.tail)
                 case Some(_) =>
                   val newBucket = Bucket(ts, Set(box))
                   val buckets0 =
                     if (buckets.size >= maxBuckets) newBucket :: buckets.init
                     else newBucket :: buckets
                   filter.set(buckets0)
                 case None =>
                   filter.set(Bucket(ts, Set(box)) :: Nil)
               }
      } yield exists
    }
  }
}
