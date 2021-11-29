package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet

object AssetsSql extends QuerySet {

  val fields: List[String] = List(
    "token_id",
    "index",
    "amount",
    "name",
    "decimals",
    "type"
  )

  val tableName: String = "assets"

}

