package org.ergoplatform.dex.domain.syntax

import cats.data.NonEmptyList
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexSellerContractParameters}
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.domain.models.OrderType.{Bid, Ask}
import org.ergoplatform.dex.domain.models.{Trade, OrderType}
import org.ergoplatform.dex.domain.syntax.ergo._

object trade {

  implicit final class MatchOps[T1 <: OrderType, T2 <: OrderType](private val m: Trade[T1, T2]) extends AnyVal {

    def orders: NonEmptyList[AnyOrder] = m.order :: m.counterOrders

    def sellerParams(implicit ev: T1 =:= OrderType.Ask): DexSellerContractParameters =
      DexSellerContractParameters(
        m.order.meta.pk,
        m.order.quoteAsset.toErgo,
        m.order.price,
        m.order.feePerToken
      )

    def buyerParams(implicit ev: T1 =:= OrderType.Bid): DexBuyerContractParameters =
      DexBuyerContractParameters(
        m.order.meta.pk,
        m.order.quoteAsset.toErgo,
        m.order.price,
        m.order.feePerToken
      )
  }

  implicit final class AnyMatchOps(private val m: AnyTrade) extends AnyVal {
    def refine: Either[Trade[Ask, Bid], Trade[Bid, Ask]] =
      Either.cond(m.order.`type`.isAsk, m.asInstanceOf[Trade[Bid, Ask]], m.asInstanceOf[Trade[Ask, Bid]])
  }
}
