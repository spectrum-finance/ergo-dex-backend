package org.ergoplatform.common.data

import cats.{Functor, Monad}
import cats.effect.Clock
import cats.effect.concurrent.Ref
import org.ergoplatform.common.data.TemporalFilter.Bucket
import org.ergoplatform.ergo.BoxId
import tofu.concurrent.MakeRef
import tofu.syntax.monadic.unit
import tofu.syntax.time._
import tofu.syntax.monadic._

import scala.annotation.tailrec
import scala.concurrent.duration.FiniteDuration

/** Temporal filter.
  * Elements are stored into temporarily distinguished buckets.
  * Up to N buckets in total. Once the limit is reached the oldest bucket is dropped.
  */
final class TemporalFilter[F[_]: Monad: Clock](
  epochLength: FiniteDuration,
  maxBuckets: Int,
  filter: Ref[F, List[Bucket]]
) {

  def inspect: F[(Int, Int)] = filter.get.map(xs => (xs.size, xs.map(_.members.size).max))

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
               case Some(bucket @ Bucket(epochStart, members)) if ts - epochStart < epochLength.toMillis =>
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

object TemporalFilter {

  final case class Bucket(epochStart: Long, members: Set[BoxId])

  def make[I[_]: Functor, F[_]: Monad: Clock](epochLength: FiniteDuration, maxBuckets: Int)(implicit
    makeRef: MakeRef[I, F]
  ): I[TemporalFilter[F]] =
    makeRef.refOf(List.empty[Bucket]).map(new TemporalFilter(epochLength, maxBuckets, _))
}
