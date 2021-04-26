package org.ergoplatform.dex.markets.sql

import doobie.implicits._
import doobie.util.log.LogHandler
import doobie.util.query.Query0
import org.ergoplatform.dex.AssetId
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

  def volumeByPair(quote: AssetId, base: AssetId)(fromTs: Long)(implicit lh: LogHandler): Query0[Long] =
    sql"""
         |select cast((sum(amount) / 2) as bigint) from fills
         |where quote_asset = $quote and base_asset = $base and ts >= $fromTs
         """.stripMargin.query

  def countTransactions(implicit lh: LogHandler): Query0[Int] =
    sql"select count(distinct tx_id) from fills".query
}
