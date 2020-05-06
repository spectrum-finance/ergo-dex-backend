package org.ergoplatform.dex.watcher.context

import org.ergoplatform.dex.domain.Order
import org.ergoplatform.dex.domain.models.Transaction
import org.ergoplatform.dex.streaming.{Consumer, Producer}
import tofu.optics.macros.{promote, ClassyOptics}

@ClassyOptics
final case class WatcherContext[F[_]](
  @promote consumer: Consumer[F, Transaction],
  @promote producer: Producer[F, Order[_]],
  subscribeTopic: String,
  publishTopic: String
)
