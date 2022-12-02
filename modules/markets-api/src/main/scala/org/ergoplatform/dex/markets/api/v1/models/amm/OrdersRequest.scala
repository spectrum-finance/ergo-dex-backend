package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.common.models.TimeInterval
import org.ergoplatform.ergo.{Address, TokenId, TxId}
import sttp.tapir.Schema

@derive(encoder, decoder)
case class OrdersRequest(
  addresses: List[Address],
  offset: Option[TimeInterval],
  orderType: Option[OrderType],
  orderStatus: Option[OrderStatus],
  assetId: Option[TokenId],
  txId: Option[TxId]
)

object OrdersRequest {
  implicit val schema: Schema[OrdersRequest] = Schema.derived
}