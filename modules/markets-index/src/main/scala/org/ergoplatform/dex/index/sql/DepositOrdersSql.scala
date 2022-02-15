package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.index.db.models.DBDeposit

object DepositOrdersSql extends QuerySet[DBDeposit] {

  val fields: List[String] = List(
    "order_id",
    "pool_id",
    "pool_state_id",
    "max_miner_fee",
    "timestamp",
    "input_id_x",
    "input_amount_x",
    "input_id_y",
    "input_amount_y",
    "output_amount_lp",
    "dex_fee",
    "redeemer",
    "protocol_version"
  )

  val tableName: String = "deposits"

}
