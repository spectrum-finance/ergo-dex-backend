package org.ergoplatform.dex.watcher.context

import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.streaming.models.Transaction
import org.ergoplatform.dex.streaming.{Consumer, Producer}
import tofu.optics.macros.{ClassyOptics, promote}

@ClassyOptics
final case class WatcherContext[F[_]](
  @promote consumer: Consumer[F, Transaction],
  @promote producer: Producer[F, AnyOrder]
)
