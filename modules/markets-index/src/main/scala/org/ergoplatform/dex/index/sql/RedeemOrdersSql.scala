package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet

object RedeemOrdersSql extends QuerySet {

  val fields: List[String] = List(
    "order_id",
    "pool_id",
    "pool_state_id",
    "max_miner_fee",
    "timestamp",
    "lp_id",
    "lp_amount",
    "lp_ticker",
    "output_amount_x",
    "output_amount_y",
    "dex_fee",
    "p2pk",
    "protocol_version"
  )

  val tableName: String = "redeems"

}
