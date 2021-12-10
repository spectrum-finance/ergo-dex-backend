package org.ergoplatform.dex.markets.sql

import doobie.util.log.LogHandler
import org.ergoplatform.common.sql.QuerySet

class AssetQueries(implicit lh: LogHandler) extends QuerySet {

  val tableName: String = "assets"

  val fields: List[String] = List(
    "id",
    "ticker",
    "description",
    "decimals"
  )
}
