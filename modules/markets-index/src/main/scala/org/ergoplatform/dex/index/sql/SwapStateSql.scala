package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.index.db.models.{DBLiquidityLock, DBSwapsState}

object SwapStateSql extends QuerySet[DBSwapsState] {

    val fields: List[String] = List(
      "address",
      "avg_time_use",
      "avg_erg_amount",
      "weight"
    )

    val tableName: String = "swaps_state"
}
