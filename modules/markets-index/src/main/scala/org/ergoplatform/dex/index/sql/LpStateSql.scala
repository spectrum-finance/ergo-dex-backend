package org.ergoplatform.dex.index.sql

import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.index.db.models.{DBLpState, PoolStateLatest}
import doobie.{Fragments, Query0}
import doobie.implicits._
import doobie.util.log.LogHandler

object LpStateSql extends QuerySet[DBLpState] {

  val tableName: String = "state"

  val fields: List[String] = List(
    "address",
    "pool_id",
    "box_id",
    "tx_id",
    "block_id",
    "balance",
    "timestamp",
    "weight",
    "op",
    "amount",
    "gap",
    "lpErg",
    "txHeight",
    "poolStateId"
  )

  def getPreviousState(address: String, poolLpToken: String)(implicit lh: LogHandler): Query0[DBLpState] =
    sql"""|select address, pool_id, box_id, tx_id, block_id, balance, timestamp, weight, op, amount, gap, lpErg, txHeight, poolStateId
          |from state where address = $address and pool_id = $poolLpToken order by id desc limit 1
          """.stripMargin.query[DBLpState]

  def getLatestPoolState(poolLpToken: String, to: Long): Query0[PoolStateLatest] =
    sql"""
          |select lp_amount, x_id, x_amount, y_id, y_amount, pool_state_id from pools where lp_id = $poolLpToken and height <= $to
          |order by height desc limit 1;
       """.stripMargin.query

}
