package org.ergoplatform.dex.markets.db.sql

import doobie.implicits._
import doobie.util.query
import doobie.util.query.Query0
import doobie.{Fragment, LogHandler}
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.amm.{ApiPool, Pools}
import org.ergoplatform.dex.markets.db.models.amm._
import org.ergoplatform.ergo.TokenId

final class AnalyticsSql(implicit lg: LogHandler) {
  val sPools = Fragment.const(Pools.pools.values.toList.map(_.unwrapped).map(s => s"'$s'").mkString(", "))

  def getUserSwapData(key: org.ergoplatform.ergo.PubKey): Query0[SwapStateUser] =
    sql"""select * from swaps_state where address=$key""".query[SwapStateUser]

  def getSummary: Query0[SwapStateSummary] =
    sql"""select sum(avg_time_use::decimal), sum(avg_erg_amount::decimal) from swaps_state""".query[SwapStateSummary]

  def getAssetTicket: Query0[AssetTicket] =
    sql"select id, ticker from assets where ticker is not null".query

  def getOffChainParticipantsCount(from: Long, to: Option[Long]): Query0[Int] =
    sql"""select count(distinct address) from order_executor_fee where timestamp > $from ${toTimestamp(to)}""".query

  def getAllOffChainAddresses(from: Long, to: Option[Long]): Query0[String] =
    sql"""select distinct address from order_executor_fee where timestamp > $from ${toTimestamp(to)}""".query

  def getOffChainState(address: String, from: Long, to: Option[Long]): Query0[OffChainOperatorState] =
    sql"""
         |select count(1), COALESCE(SUM(fee),0) from order_executor_fee where address = $address and timestamp > $from ${toTimestamp(
      to
    )}
       """.stripMargin.query

  def getTotalOffChainOperationsCount(from: Long, to: Option[Long]): Query0[Int] =
    sql"""
         |select count(1) from order_executor_fee where timestamp > $from ${toTimestamp(to)}
       """.stripMargin.query

  def toTimestamp(to: Option[Long]): Fragment =
    to.map(to => sql"and timestamp < $to").getOrElse(Fragment.empty)

  def checkIfBetaTester(key: org.ergoplatform.ergo.PubKey): Query0[Int] = {
    val poolsIds = Fragment.const(
      (Pools.betaTesterPools ::: Pools.pools.values.toList)
        .map(_.unwrapped)
        .map(s => s"'$s'")
        .mkString(", ")
    )

    sql"""
         |select count(distinct addr) from (
         |    select DISTINCT redeemer from swaps where timestamp > 1628640000000 and timestamp < 1636502400000
         |     and redeemer = $key
         |     and pool_id in ($poolsIds)
         |        union
         |    select DISTINCT redeemer from redeems where timestamp > 1628640000000 and timestamp < 1636502400000
         |      and redeemer = $key
         |      and pool_id in ($poolsIds)
         |        union
         |    select DISTINCT redeemer from deposits where timestamp > 1628640000000 and timestamp < 1636502400000
         |      and redeemer = $key
         |      and pool_id in ($poolsIds)
         |) addr;
       """.stripMargin.query
  }

  def getSwapsState(key: org.ergoplatform.ergo.PubKey): Query0[SwapState] =
    sql"""
         |select input_id, input_value, output_amount
         |from swaps where timestamp > 1633910400000 and redeemer = $key and
         |pool_id in ($sPools) and output_amount is not null
         |""".stripMargin.query

  def getTotalWeight: Query0[BigDecimal] =
    sql"""
         |select COALESCE(sum(weight::decimal), 0) from state
         |where timestamp > 1633910400000 and amount != 9223372036854774807
         |and pool_id in ($sPools)
         """.stripMargin.query

  def getSwapUsersCount: Query0[Int] =
    sql"""
         |select count(distinct redeemer) from swaps
         |where timestamp > 1633910400000 and output_amount is not null and pool_id in ($sPools)
       """.stripMargin.query

  def getLqUsers: Query0[Int] = {
    val s = ApiPool.values.toList.map(_.poolLp).map(s => s"'$s'").mkString(",")
    sql"""
          |select count(distinct address) from state
          |where timestamp > 1633910400000 and amount != 9223372036854774807
          |and pool_id in ($sPools)
       """.stripMargin.query
  }

  def getLqProviderState(address: String, pool: String): Query0[LqProviderStateDB] =
    sql"""
         |select address, COALESCE(sum(weight::decimal), 0), count(1), COALESCE(sum(lpErg::decimal), 0), COALESCE(sum(gap), 0) from state
         |where address = $address and pool_id = $pool and amount != 9223372036854774807
         |group by address
         |""".stripMargin.query

  def getLqProviderStates(address: String, pool: String, from: Long, to: Long): Query0[DBLpState] =
    sql"""
         |select pool_id, tx_id, balance, timestamp, op, amount from state
         |where address = $address and pool_id = $pool and timestamp > $from and timestamp < $to
         |and amount != 9223372036854774807
         |""".stripMargin.query

  def getInfo(id: PoolId): Query0[PoolInfo] =
    sql"""
         |select s.timestamp from swaps s where s.pool_id = $id order by s.timestamp asc limit 1
         """.stripMargin.query

  def getPoolSnapshot(id: PoolId): Query0[PoolSnapshot] =
    sql"""
         |select p.pool_id, p.x_id, p.x_amount, ax.ticker, ax.decimals, p.y_id, p.y_amount, ay.ticker, ay.decimals, 0
         |from pools p
         |left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id
         |where p.pool_id = $id
         |order by p.gindex desc limit 1;
         """.stripMargin.query[PoolSnapshot]

  def getPoolSnapshots(hasTicker: Boolean): Query0[PoolSnapshot] = {

    val assetJoin =
      if (hasTicker)
        Fragment.const(
          """left join (
        | select * from assets where ticker is not null 
        |) ax on ax.id = p.x_id
        |left join (
        |select * from assets where ticker is not null
        |) ay on ay.id = p.y_id
        |""".stripMargin
        )
      else
        Fragment.const(
          """left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id""".stripMargin
        )

    sql"""
         |select p.pool_id, p.x_id, p.x_amount, ax.ticker, ax.decimals, p.y_id, p.y_amount, ay.ticker, ay.decimals, (p.x_amount::decimal) * p.y_amount as lq
         |from pools p
         |left join (
         |	select pool_id, max(gindex) as gindex
         |	from pools
         |	group by pool_id
         |) as px on p.pool_id = px.pool_id and p.gindex = px.gindex
         $assetJoin
         |where px.gindex = p.gindex
         |order by lq desc
         """.stripMargin.query[PoolSnapshot]
  }

  def getPoolSnapshotsByAsset(asset: TokenId): Query0[PoolSnapshot] =
    sql"""
         |select p.pool_id, p.x_id, p.x_amount, ax.ticker, ax.decimals, p.y_id, p.y_amount, ay.ticker, ay.decimals, (p.x_amount::decimal) * p.y_amount as lq
         |from pools p
         |left join (
         |	select pool_id, max(gindex) as gindex
         |	from pools
         |  where x_id = $asset or y_id = $asset
         |	group by pool_id
         |) as px on p.pool_id = px.pool_id and p.gindex = px.gindex
         |left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id
         |where px.gindex = p.gindex
         |order by lq desc
         """.stripMargin.query[PoolSnapshot]

  def getPoolVolumes(tw: TimeWindow): Query0[PoolVolumeSnapshot] =
    sql"""
         |select distinct on (p.pool_id)
         |  p.pool_id,
         |  p.x_id,
         |  sx.tx,
         |  ax.ticker,
         |  ax.decimals,
         |  p.y_id,
         |  sx.ty,
         |  ay.ticker,
         |  ay.decimals
         |from pools p
         |left join (
         |  select
         |    s.pool_id,
         |    cast(sum(case when (s.input_id = p.y_id) then s.output_amount else 0 end) as BIGINT) as tx,
         |    cast(sum(case when (s.input_id = p.x_id) then s.output_amount else 0 end) as BIGINT) as ty
         |  from swaps s
         |  left join pools p on p.pool_state_id = s.pool_state_id
         |  ${timeWindowCond(tw, "where", "s")}
         |  group by s.pool_id
         |) as sx on sx.pool_id = p.pool_id
         |left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id
         |where sx.pool_id is not null
         """.stripMargin.query[PoolVolumeSnapshot]

  def getPoolVolumes(id: PoolId, tw: TimeWindow): Query0[PoolVolumeSnapshot] = {
    val tsCond =
      Fragment.const(
        s"${tw.from.map(ts => s"and s.timestamp >= $ts").getOrElse("")} ${tw.to.map(ts => s"and s.timestamp <= $ts").getOrElse("")}"
      )
    sql"""
         |select distinct on (p.pool_id)
         |  p.pool_id,
         |  p.x_id,
         |  sx.tx,
         |  ax.ticker,
         |  ax.decimals,
         |  p.y_id,
         |  sx.ty,
         |  ay.ticker,
         |  ay.decimals
         |from pools p
         |left join (
         |  select
         |    s.pool_id,
         |    cast(sum(case when (s.input_id = p.y_id) then s.output_amount else 0 end) as BIGINT) as tx,
         |    cast(sum(case when (s.input_id = p.x_id) then s.output_amount else 0 end) as BIGINT) as ty
         |  from swaps s
         |  left join pools p on p.pool_state_id = s.pool_state_id
         |  where s.pool_id = $id $tsCond
         |  group by s.pool_id
         |) as sx on sx.pool_id = p.pool_id
         |left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id
         |where sx.pool_id is not null
         """.stripMargin.query[PoolVolumeSnapshot]
  }

  def getPoolFees(tw: TimeWindow): Query0[PoolFeesSnapshot] =
    sql"""
         |select distinct on (p.pool_id)
         |  p.pool_id,
         |  p.x_id,
         |  sx.tx,
         |  ax.ticker,
         |  ax.decimals,
         |  p.y_id,
         |  sx.ty,
         |  ay.ticker,
         |  ay.decimals
         |from pools p
         |left join (
         |  select
         |    s.pool_id,
         |    cast(sum(case when (s.input_id = p.y_id) then s.output_amount::decimal * (1000 - p.fee_num) / 1000 else 0 end) as bigint) as tx,
         |    cast(sum(case when (s.input_id = p.x_id) then s.output_amount::decimal * (1000 - p.fee_num) / 1000 else 0 end) as bigint) as ty
         |  from swaps s
         |  left join pools p on p.pool_state_id = s.pool_state_id
         |  ${timeWindowCond(tw, "where", "s")}
         |  group by s.pool_id
         |) as sx on sx.pool_id = p.pool_id
         |left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id
         |where sx.pool_id is not null
         """.stripMargin.query[PoolFeesSnapshot]

  def getPoolFees(poolId: PoolId, tw: TimeWindow): Query0[PoolFeesSnapshot] = {
    val tsCond =
      Fragment.const(
        s"${tw.from.map(ts => s"and s.timestamp >= $ts").getOrElse("")} ${tw.to.map(ts => s"and s.timestamp <= $ts").getOrElse("")}"
      )
    sql"""
         |select distinct on (p.pool_id)
         |  p.pool_id,
         |  p.x_id,
         |  sx.tx,
         |  ax.ticker,
         |  ax.decimals,
         |  p.y_id,
         |  sx.ty,
         |  ay.ticker,
         |  ay.decimals
         |from pools p
         |left join (
         |  select
         |    s.pool_id,
         |    cast(sum(case when (s.input_id = p.y_id) then s.output_amount::decimal * (1000 - p.fee_num) / 1000 else 0 end) as bigint) as tx,
         |    cast(sum(case when (s.input_id = p.x_id) then s.output_amount::decimal * (1000 - p.fee_num) / 1000 else 0 end) as bigint) as ty
         |  from swaps s
         |  left join pools p on p.pool_state_id = s.pool_state_id
         |  where p.pool_id = $poolId $tsCond
         |  group by s.pool_id
         |) as sx on sx.pool_id = p.pool_id
         |left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id
         |where sx.pool_id is not null
         """.stripMargin.query[PoolFeesSnapshot]
  }

  def getPrevPoolTrace(id: PoolId, depth: Int, currHeight: Int): Query0[PoolTrace] =
    sql"""
         |select p.pool_id, p.x_id, p.x_amount, ax.ticker, ax.decimals, p.y_id, p.y_amount, ay.ticker, ay.decimals, p.height, p.gindex
         |from pools p
         |left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id
         |where p.height < $currHeight - $depth
         |and p.pool_id = $id
         |order by p.gindex desc
         |limit 1
         """.stripMargin.query[PoolTrace]

  def getPoolTrace(id: PoolId, depth: Int, currHeight: Int): Query0[PoolTrace] =
    sql"""
         |select p.pool_id, p.x_id, p.x_amount, ax.ticker, ax.decimals, p.y_id, p.y_amount, ay.ticker, ay.decimals, p.height, p.gindex
         |from pools p
         |left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id
         |where p.pool_id = $id
         |and p.height >= $currHeight - $depth
         """.stripMargin.query[PoolTrace]

  def getAvgPoolSnapshot(id: PoolId, tw: TimeWindow, resolution: Int): Query0[AvgAssetAmounts] =
    sql"""
         |select avg(p.x_amount) as avg_x_amount, avg(p.y_amount) as avg_y_amount, avg(b.timestamp), ((p.height / $resolution)::integer) as k
         |from pools p
         |left join blocks b on b.height = p.height
         |where pool_id = $id and b.height is not null and ${blockTimeWindowMapping(tw)}
         |group by k
         |order by k
         """.stripMargin.query[AvgAssetAmounts]

  def getAssetById(id: TokenId): Query0[AssetInfo] =
    sql"""
         |select id, ticker, decimals from assets
         |where id = $id
         """.stripMargin.query[AssetInfo]

  def getSwapTransactions(tw: TimeWindow): Query0[SwapInfo] =
    sql"""
         |select s.min_output_id, s.output_amount, a.ticker, a.decimals,
         |(select count(*) from swaps sx where output_amount is not null ${timeWindowCond(tw, "and", "sx")}) as numTxs from swaps s
         |left join assets a on a.id = s.min_output_id
         |where s.output_amount is not null
         |${timeWindowCond(tw, "and", "s")}
         """.stripMargin.query[SwapInfo]

  def getDepositTransactions(tw: TimeWindow): Query0[DepositInfo] =
    sql"""
         |select s.input_id_x, s.input_amount_x, ax.ticker, ax.decimals, s.input_id_y, s.input_amount_y, ay.ticker, ay.decimals,
         |(select count(*) from deposits sx where output_amount_lp is not null ${timeWindowCond(tw, "and", "sx")}) as numTxs from deposits s
         |left join assets ax on ax.id = s.input_id_x  
         |left join assets ay on ay.id = s.input_id_y
         |where output_amount_lp is not null
         |${timeWindowCond(tw, "and", "s")}
         """.stripMargin.query[DepositInfo]

  private def timeWindowCond(tw: TimeWindow, condKeyword: String, alias: String): Fragment =
    if (tw.from.nonEmpty || tw.to.nonEmpty)
      Fragment.const(
        s"$condKeyword ${tw.from.map(ts => s"$alias.timestamp >= $ts").getOrElse("")} ${if (tw.from.isDefined && tw.to.isDefined) "and"
        else ""} ${tw.to.map(ts => s"$alias.timestamp <= $ts").getOrElse("")}"
      )
    else Fragment.empty

  private def blockTimeWindowMapping(tw: TimeWindow): Fragment =
    if (tw.from.nonEmpty || tw.to.nonEmpty)
      Fragment.const(
        s"${tw.from.map(ts => s"b.timestamp >= $ts").getOrElse("")} ${if (tw.from.isDefined && tw.to.isDefined) "and"
        else ""} ${tw.to.map(ts => s"b.timestamp <= $ts").getOrElse("")}"
      )
    else Fragment.const("true")
}
