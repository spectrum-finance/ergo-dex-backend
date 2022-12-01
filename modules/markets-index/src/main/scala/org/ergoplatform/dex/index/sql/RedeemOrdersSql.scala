package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.index.db.models.DBRedeem

object RedeemOrdersSql extends QuerySet[DBRedeem] {

  val fields: List[String] = List(
    "order_id",
    "pool_id",
    "pool_state_id",
    "max_miner_fee",
    "timestamp",
    "lp_id",
    "lp_amount",
    "output_amount_x",
    "output_amount_y",
    "dex_fee",
    "redeemer",
    "protocol_version",
    "contract_version",
    "redeemer_ergo_tree"
  )

  val tableName: String = "redeems"

}
