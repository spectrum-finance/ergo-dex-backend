package org.ergoplatform.dex.matcher.streaming

import org.ergoplatform.dex.{OrderId, TradeId}
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.streaming.{Consumer, Producer}

final case class StreamingBundle[F[_], G[_]](
  producer: Producer[TradeId, AnyTrade, F],
  consumer: Consumer[OrderId, AnyOrder, F, G]
)
