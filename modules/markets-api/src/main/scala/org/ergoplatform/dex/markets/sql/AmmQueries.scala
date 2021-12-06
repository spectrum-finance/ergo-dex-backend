package org.ergoplatform.dex.markets.sql

import doobie.implicits._
import doobie.util.log.LogHandler
import doobie.util.query.Query0
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.common.sql.QuerySet

class AmmQueries(implicit lh: LogHandler) {

  def volumeByPair(quote: TokenId, base: TokenId)(fromTs: Long): Query0[Long] =
    sql"""
         |select cast((sum(amount) / 2) as bigint) from fills
         |where quote_asset = $quote and base_asset = $base and ts >= $fromTs
         """.stripMargin.query

  def countTransactions(implicit lh: LogHandler): Query0[Int] =
    sql"select count(distinct tx_id) from fills".query
}
