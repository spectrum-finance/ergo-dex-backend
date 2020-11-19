package org.ergoplatform.dex.domain.syntax

import cats.syntax.either._
import org.ergoplatform.dex.domain.models.OrderType.{Ask, Bid}
import org.ergoplatform.dex.domain.models.Trade
import org.ergoplatform.dex.domain.models.Trade.AnyTrade

object trade {

  implicit final class AnyMatchOps(private val trade: AnyTrade) extends AnyVal {

    def refine: Either[Trade[Bid, Ask], Trade[Ask, Bid]] =
      trade match {
        case t: Trade[Ask, Bid] @unchecked if trade.order.`type`.isAsk => t.asRight
        case t: Trade[Bid, Ask] @unchecked                             => t.asLeft
      }
  }
}
