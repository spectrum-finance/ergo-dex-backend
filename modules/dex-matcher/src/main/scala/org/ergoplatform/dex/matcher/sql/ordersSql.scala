package org.ergoplatform.dex.matcher.sql

import cats.data.NonEmptyList
import doobie.implicits._
import doobie.util.log.LogHandler
import doobie.util.query.Query0
import doobie.util.update.Update0
import doobie.{Fragments, Update}
import org.ergoplatform.dex.domain.orderbook.Order.{AnyOrder, Ask, Bid}
import org.ergoplatform.dex.protocol.instances._
import org.ergoplatform.dex.sql.QuerySet
import org.ergoplatform.dex.{OrderId, PairId}

object ordersSql extends QuerySet {

  val tableName: String = "orders"

  val fields: List[String] =
    List(
      "type",
      "quote_asset",
      "base_asset",
      "amount",
      "price",
      "fee_per_token",
      "box_id",
      "box_value",
      "script",
      "pk",
      "ts"
    )

  def getBuyWall(pairId: PairId)(implicit lh: LogHandler): Query0[Bid] =
    sql"""
       |select
       |  o.type,
       |  o.quote_asset,
       |  o.base_asset,
       |  o.amount,
       |  o.price,
       |  o.fee_per_token,
       |  o.box_id,
       |  o.box_value,
       |  o.script,
       |  o.pk,
       |  o.ts
       |from orders o
       |where
       |  o.type = 'bid' and
       |  o.quote_asset = ${pairId.quoteId} and
       |  o.base_asset = ${pairId.baseId}
       """.stripMargin.query[Bid]

  def getSellWall(pairId: PairId)(implicit lh: LogHandler): Query0[Ask] =
    sql"""
         |select
         |  o.type,
         |  o.quote_asset,
         |  o.base_asset,
         |  o.amount,
         |  o.price,
         |  o.fee_per_token,
         |  o.box_id,
         |  o.box_value,
         |  o.script,
         |  o.pk,
         |  o.ts
         |from orders o
         |where
         |  o.type = 'ask' and
         |  o.quote_asset = ${pairId.quoteId} and
         |  o.base_asset = ${pairId.baseId}
       """.stripMargin.query[Ask]

  def remove(ids: NonEmptyList[OrderId])(implicit lh: LogHandler): Update0 =
    Fragments.in(sql"delete from orders where box_id ", ids).update
}
