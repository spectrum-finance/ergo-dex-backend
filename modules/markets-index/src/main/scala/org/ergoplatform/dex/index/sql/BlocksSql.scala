package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.index.db.models.DBBlock

object BlocksSql extends QuerySet[DBBlock] {

  val fields: List[String] = List(
    "id",
    "height",
    "timestamp"
  )

  val tableName: String = "blocks"
}