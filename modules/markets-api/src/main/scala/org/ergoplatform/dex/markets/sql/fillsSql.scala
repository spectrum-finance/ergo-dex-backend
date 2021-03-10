package org.ergoplatform.dex.markets.sql

import doobie.implicits._
import doobie.util.log.LogHandler
import doobie.util.query.Query0
import org.ergoplatform.dex.sql.QuerySet

object fillsSql extends QuerySet {

  val tableName: String = "fills"

  val fields: List[String] =
    List(
      "side",
      "tx_id",
      "height",
      "quote_asset",
      "base_asset",
      "amount",
      "price",
      "fee",
      "ts"
    )

  def countTransactions(implicit lh: LogHandler): Query0[Int] =
    sql"select count(distinct tx_id) from fills".query
}
