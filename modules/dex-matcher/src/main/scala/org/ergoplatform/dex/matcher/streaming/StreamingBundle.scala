package org.ergoplatform.dex.matcher.streaming

import org.ergoplatform.dex.domain.models.Match.AnyMatch
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.streaming.{Consumer, Producer}

final case class StreamingBundle[F[_]](
  producer: Producer[F, AnyMatch],
  consumer: Consumer[F, AnyOrder]
)
