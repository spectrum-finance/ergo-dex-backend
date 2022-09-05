package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.index.db.models.{DBLiquidityLock, DBOffChainOperator}

object OffChainOperatorSql extends QuerySet[DBOffChainOperator] {

  val fields: List[String] = List(
    "output_id",
    "fee",
    "order_id",
    "address",
    "timestamp"
  )

  val tableName: String = "off_chain_operators"
}
