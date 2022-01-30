package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.index.db.models.DBLiquidityLock

object LqLocksSql extends QuerySet[DBLiquidityLock] {

  val fields: List[String] = List(
    "id",
    "deadline",
    "token_id",
    "amount",
    "redeemer"
  )

  val tableName: String = "lq_locks"
}
