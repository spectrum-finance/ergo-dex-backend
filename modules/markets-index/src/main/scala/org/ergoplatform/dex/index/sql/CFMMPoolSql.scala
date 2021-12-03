package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet

object CFMMPoolSql extends QuerySet {

  val fields: List[String] = List(
    "pool_id",
    "lp_id",
    "lp_amount",
    "lp_ticker",
    "x_id",
    "x_amount",
    "x_ticker",
    "y_id",
    "y_amount",
    "y_ticker",
    "fee_num",
    "box_id"
  )

  val tableName: String = "pools"

}
