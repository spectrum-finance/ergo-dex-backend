package org.ergoplatform.dex.markets.sql

import org.ergoplatform.dex.sql.QuerySet

object tradesSql extends QuerySet {

  val tableName: String = "trades"

  val fields: List[String] =
    List(
      "tx_id",
      "quote_asset",
      "base_asset",
      "amount",
      "price",
      "fee",
      "ts"
    )
}
