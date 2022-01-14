package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet

object SwapOrdersSql extends QuerySet {

  val fields: List[String] = List(
    "order_id",
    "pool_id",
    "pool_state_id",
    "max_miner_fee",
    "timestamp",
    "input_id",
    "input_value",
    "min_output_id",
    "min_output_amount",
    "output_amount",
    "dex_fee_per_token_num",
    "dex_fee_per_token_denom",
    "p2pk",
    "protocol_version"
  )

  val tableName: String = "swaps"
}
