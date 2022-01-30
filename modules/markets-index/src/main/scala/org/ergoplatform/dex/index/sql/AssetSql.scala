package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.index.db.models.DBAssetInfo

object AssetSql extends QuerySet[DBAssetInfo] {

  val fields: List[String] = List(
    "id",
    "ticker",
    "decimals"
  )

  val tableName: String = "assets"
}
