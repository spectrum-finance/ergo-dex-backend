package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.index.db.models.DBPool

object CFMMPoolSql extends QuerySet[DBPool] {

  val fields: List[String] = List(
    "pool_state_id",
    "pool_id",
    "lp_id",
    "lp_amount",
    "x_id",
    "x_amount",
    "y_id",
    "y_amount",
    "fee_num",
    "gindex",
    "protocol_version"
  )

  val tableName: String = "pools"
}
