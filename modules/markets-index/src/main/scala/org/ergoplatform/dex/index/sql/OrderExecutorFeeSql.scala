package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.index.db.models.DBOrderExecutorFee

object OrderExecutorFeeSql extends QuerySet[DBOrderExecutorFee] {

  val fields: List[String] = List(
    "pool_id",
    "order_id",
    "output_id",
    "address",
    "fee",
    "timestamp"
  )

  val tableName: String = "order_executor_fee"
}
