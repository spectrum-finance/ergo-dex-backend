package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet

object DepositOrdersSql extends QuerySet {

  val fields: List[String] = List(
    "pool_id",
    "max_miner_fee",
    "timestamp",
    "in_x_id",
    "in_x_value",
    "in_x_ticker",
    "in_y_id",
    "in_y_value",
    "in_y_ticker",
    "dex_fee",
    "p2pk"
  )

  val tableName: String = "deposits"

}
