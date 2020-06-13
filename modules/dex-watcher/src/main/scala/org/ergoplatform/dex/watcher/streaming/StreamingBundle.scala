package org.ergoplatform.dex.watcher.streaming

import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.streaming.models.Transaction
import org.ergoplatform.dex.streaming.{Consumer, Producer}

final case class StreamingBundle[F[_]](
  producer: Producer[F, AnyOrder],
  consumer: Consumer[F, Transaction]
)
