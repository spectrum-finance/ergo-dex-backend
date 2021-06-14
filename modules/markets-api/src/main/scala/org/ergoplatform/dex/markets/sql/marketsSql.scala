package org.ergoplatform.dex.markets.sql

import org.ergoplatform.common.sql.QuerySet

object marketsSql extends QuerySet {

  val tableName: String = "markets"

  val fields: List[String] =
    List(
      "quote_asset",
      "base_asset",
      "ticker"
    )
}
