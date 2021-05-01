package org.ergoplatform.dex.matcher.streaming

import org.ergoplatform.dex.domain.orderbook.Trade.AnyTrade
import org.ergoplatform.dex.domain.orderbook.Order.AnyOrder
import org.ergoplatform.dex.domain.orderbook.{OrderId, TradeId}
import org.ergoplatform.dex.streaming.{Consumer, Producer}

final case class StreamingBundle[F[_], G[_]](
  consumer: Consumer[OrderId, AnyOrder, F, G],
  producer: Producer[TradeId, AnyTrade, F]
)
