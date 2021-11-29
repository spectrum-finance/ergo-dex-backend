package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet

object RedeemOrdersSql extends QuerySet {

  val fields: List[String] = List(
    "pool_id",
    "max_miner_fee",
    "timestamp",
    "lp_id",
    "lp_value",
    "lp_ticker",
    "dex_fee",
    "p2pk"
  )

  val tableName: String = "redeems"

}
