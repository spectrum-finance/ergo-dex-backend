package org.ergoplatform.dex.index.sql

import doobie.Query0
import doobie.implicits._
import doobie.util.log.LogHandler
import org.ergoplatform.common.sql.QuerySet
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.index.db.models.{LiquidityProviderSnapshot, PoolSnapshot, UnresolvedState}

object LiquidityProvidersSql extends QuerySet[LiquidityProviderSnapshot] {

  val tableName: String = "state"

  val fields: List[String] = List(
    "address",
    "pool_id",
    "lp_id",
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

  def getLatestLiquidityProviderSnapshot(address: String, poolId: PoolId)(implicit
    lh: LogHandler
  ): Query0[LiquidityProviderSnapshot] =
    sql"""|select address, pool_id, lp_id, box_id, tx_id, block_id, balance, timestamp, weight, op, amount, gap, lpErg, txHeight, poolStateId from state
          |where address = $address and pool_id = $poolId order by id desc limit 1
          """.stripMargin.query[LiquidityProviderSnapshot]

  def getLatestPoolSnapshot(poolId: PoolId, to: Long): Query0[PoolSnapshot] =
    sql"""
          |select lp_amount, x_id, x_amount, y_id, y_amount, pool_state_id from pools
          |where pool_id = $poolId and height <= $to
          |order by height desc limit 1;
       """.stripMargin.query

  def getAllUnresolvedStates: Query0[UnresolvedState] =
    sql"""
         |select s1.address, s1.pool_id, s1.lp_id, s1.balance, s1.lperg, s1.timestamp, s1.box_id from state s1
         |  left join (
         |    select address, pool_id, max(id) as id
         |    from state
         |    where balance::decimal != 0 GROUP by address, pool_id
         |  ) s2 on s2.id = s1.id
         |where s1.id = s2.id"""
      .stripMargin.query

}
