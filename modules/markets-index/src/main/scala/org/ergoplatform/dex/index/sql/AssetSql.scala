package org.ergoplatform.dex.index.sql

import cats.data.NonEmptyList
import doobie.{Fragments, Query0}
import doobie.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.index.db.models.DBAssetInfo

object AssetSql extends QuerySet[DBAssetInfo] {

  val fields: List[String] = List(
    "id",
    "ticker",
    "decimals"
  )

  val tableName: String = "assets"

  def existing(tokenIds: NonEmptyList[TokenId])(implicit lh: LogHandler): Query0[TokenId] =
    Fragments.in(sql"select id from assets where id", tokenIds).query
}
