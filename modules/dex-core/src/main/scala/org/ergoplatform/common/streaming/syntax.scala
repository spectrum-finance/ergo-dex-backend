package org.ergoplatform.common.streaming

import cats.data.Chain
import cats.instances.option._
import cats.syntax.foldable._
import cats.{Applicative, Foldable}
import tofu.streams.{Evals, Temporal}
import tofu.syntax.streams.evals._
import tofu.syntax.streams.temporal._

import scala.concurrent.duration.FiniteDuration

object syntax {

  implicit final class CommittableOps[S[_], F[_], K, V, O](private val fa: S[Committable[K, V, O, F]]) extends AnyVal {

    def commitBatchWithin[C[_]: Foldable](n: Int, d: FiniteDuration)(implicit
      T: Temporal[S, C],
      S: Evals[S, F],
      F: Applicative[F]
    ): S[Unit] =
      fa.groupWithin(n, d).evalMap { batch =>
        val commit = batch.get(0).map(_.commitBatch)
        commit.traverse_(_(batch.foldLeft(Chain.empty[O])(_ append _.offset)))
      }
  }
}
