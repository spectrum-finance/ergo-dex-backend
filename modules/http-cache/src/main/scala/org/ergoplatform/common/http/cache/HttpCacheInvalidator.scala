package org.ergoplatform.common.http.cache

import cats.{Foldable, Functor, Monad}
import org.ergoplatform.dex.index.streaming.BlocksConsumer
import tofu.streams.{Chunks, Evals, Temporal}
import tofu.syntax.streams.all.toEvalsOps
import org.ergoplatform.common.streaming.syntax._

import scala.concurrent.duration._

trait HttpCacheInvalidator[F[_]] {
  def run: F[Unit]
}

object HttpCacheInvalidator {

  val BatchSize          = 128
  val BatchCommitTimeout = 1.second

  def make[
    S[_]: Evals[*[_], F]: Chunks[*[_], C]: Temporal[*[_], C],
    F[_]: Monad,
    C[_]: Functor: Foldable
  ](implicit
    caching: HttpResponseCaching[F],
    blocks: BlocksConsumer[S, F]
  ): HttpCacheInvalidator[S] =
    new Invalidator[S, F, C](caching, blocks)

  final class Invalidator[
    S[_]: Evals[*[_], F]: Chunks[*[_], C]: Temporal[*[_], C],
    F[_]: Monad,
    C[_]: Functor: Foldable
  ](
    caching: HttpResponseCaching[F],
    blocks: BlocksConsumer[S, F]
  ) extends HttpCacheInvalidator[S] {

    def run: S[Unit] =
      blocks.stream
        .evalTap(_ => caching.invalidateAll)
        .commitBatchWithin(BatchSize, BatchCommitTimeout)
  }
}
