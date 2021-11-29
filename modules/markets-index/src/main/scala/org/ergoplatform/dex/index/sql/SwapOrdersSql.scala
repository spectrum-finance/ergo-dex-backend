package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet

object SwapOrdersSql extends QuerySet {

  val fields: List[String] = List(
    "pool_id",
    "max_miner_fee",
    "timestamp",
    "input_id",
    "input_value",
    "input_ticker",
    "min_output_id",
    "min_output_value",
    "min_output_ticker",
    "dex_fee_per_token_num",
    "dex_fee_per_token_denom",
    "p2pk"
  )

  val tableName: String = "swaps"
}
