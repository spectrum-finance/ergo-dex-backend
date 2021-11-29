package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet

object OutputsSql extends QuerySet {

  val fields: List[String] = List(
    "box_id",
    "transaction_id",
    "value",
    "index",
    "global_index",
    "creation_height",
    "settlement_height",
    "ergo_tree",
    "address",
    "additional_registers"
  )

  val tableName: String = "outputs"

}
