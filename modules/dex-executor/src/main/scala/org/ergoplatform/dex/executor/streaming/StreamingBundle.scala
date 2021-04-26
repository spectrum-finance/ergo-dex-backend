package org.ergoplatform.dex.executor.streaming

import org.ergoplatform.dex.domain.orderbook.Order.AnyOrder
import org.ergoplatform.dex.{OrderId, TradeId}
import org.ergoplatform.dex.domain.orderbook.Trade.AnyTrade
import org.ergoplatform.dex.streaming.{Consumer, Producer}

final case class StreamingBundle[F[_], G[_]](
  consumer: Consumer[TradeId, AnyTrade, F, G],
  producer: Producer[OrderId, AnyOrder, F]
)
