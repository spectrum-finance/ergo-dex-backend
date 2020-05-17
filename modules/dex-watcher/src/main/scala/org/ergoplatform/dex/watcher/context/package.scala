package org.ergoplatform.dex.watcher

import org.ergoplatform.dex.domain.Order
import org.ergoplatform.dex.streaming.models.Transaction
import org.ergoplatform.dex.streaming.{Consumer, Producer}
import tofu.{Context, HasContext}
import tofu.optics.Extract

package object context {

  type HasWatcherContext[F[_]] = F HasContext WatcherContext[F]

  @inline def askConsumer[F[_]: HasWatcherContext](
    implicit lens: WatcherContext[F] Extract Consumer[F, Transaction]
  ): F[Consumer[F, Transaction]] =
    Context[F].extract(lens).context

  @inline def askProducer[F[_]: HasWatcherContext](
    implicit lens: WatcherContext[F] Extract Producer[F, Order]
  ): F[Producer[F, Order]] =
    Context[F].extract(lens).context
}
