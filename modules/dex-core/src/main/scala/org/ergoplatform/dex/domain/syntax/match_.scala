package org.ergoplatform.dex.domain.syntax

import cats.data.NonEmptyList
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexSellerContractParameters}
import org.ergoplatform.dex.domain.models.Match.AnyMatch
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.domain.models.OrderType.{Buy, Sell}
import org.ergoplatform.dex.domain.models.{Match, OrderType}
import org.ergoplatform.dex.domain.syntax.ergo._

object match_ {

  implicit final class MatchOps[T1 <: OrderType, T2 <: OrderType](private val m: Match[T1, T2]) extends AnyVal {

    def allOrders: NonEmptyList[AnyOrder] = m.order :: m.counterOrders

    def sellerParams(implicit ev: T1 =:= OrderType.Sell): DexSellerContractParameters =
      DexSellerContractParameters(
        m.order.meta.ownerAddress.pubkey,
        m.order.asset.toErgo,
        m.order.price,
        m.order.feePerToken
      )

    def buyerParams(implicit ev: T1 =:= OrderType.Buy): DexBuyerContractParameters =
      DexBuyerContractParameters(
        m.order.meta.ownerAddress.pubkey,
        m.order.asset.toErgo,
        m.order.price,
        m.order.feePerToken
      )
  }

  implicit final class AnyMatchOps(private val m: AnyMatch) extends AnyVal {
    def refine: Either[Match[Sell, Buy], Match[Buy, Sell]] =
      Either.cond(m.order.`type`.isSell, m.asInstanceOf[Match[Buy, Sell]], m.asInstanceOf[Match[Sell, Buy]])
  }
}
