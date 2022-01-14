package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet

object AssetSql extends QuerySet {

  val fields: List[String] = List(
    "id",
    "ticker",
    "decimals"
  )

  val tableName: String = "assets"
}
