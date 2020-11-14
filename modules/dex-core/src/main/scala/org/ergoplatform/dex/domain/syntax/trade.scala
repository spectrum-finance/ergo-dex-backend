package org.ergoplatform.dex.domain.syntax

import org.ergoplatform.dex.domain.models.OrderType.{Ask, Bid}
import org.ergoplatform.dex.domain.models.Trade
import org.ergoplatform.dex.domain.models.Trade.AnyTrade

object trade {

  implicit final class AnyMatchOps(private val trade: AnyTrade) extends AnyVal {

    def refine: Either[Trade[Bid, Ask], Trade[Ask, Bid]] =
      Either.cond(trade.order.`type`.isAsk, trade.asInstanceOf[Trade[Ask, Bid]], trade.asInstanceOf[Trade[Bid, Ask]])
  }
}
