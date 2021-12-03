package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet

object DepositOrdersSql extends QuerySet {

  val fields: List[String] = List(
    "order_id",
    "pool_id",
    "pool_state_id",
    "max_miner_fee",
    "timestamp",
    "input_id_x",
    "input_amount_x",
    "input_ticker_x",
    "input_id_y",
    "input_amount_y",
    "input_ticker_y",
    "output_amount_lp",
    "dex_fee",
    "p2pk"
  )

  val tableName: String = "deposits"

}
