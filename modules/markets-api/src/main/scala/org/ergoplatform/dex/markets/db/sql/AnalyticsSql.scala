package org.ergoplatform.dex.markets.db.sql

import doobie.LogHandler
import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.query.Query0
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.db.models.{PoolFeesSnapshot, PoolSnapshot, PoolVolumeSnapshot}

class AnalyticsSql(implicit lg: LogHandler) {

  def getPoolSnapshots: Query0[PoolSnapshot] =
    sql"""
         |select p.pool_id, p.x_id, p.x_amount, ax.ticker, ax.decimals, p.y_id, p.y_amount, ay.ticker, ay.decimals
         |from pools p
         |left join (
         |	select pool_id, max(gindex) as gindex
         |	from pools
         |	group by pool_id
         |) as px on p.pool_id = px.pool_id and p.gindex = px.gindex
         |left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id
         |where px.gindex = p.gindex
         """.stripMargin.query[PoolSnapshot]

  def getPoolVolumes(tw: TimeWindow): Query0[PoolVolumeSnapshot] = {
    val tsCond =
      Fragment.const(
        s"where ${tw.from.map(ts => s"s.timestamp >= $ts").getOrElse("")} ${if (tw.from.isDefined && tw.to.isDefined) "and"
        else ""} ${tw.to.map(ts => s"s.timestamp <= $ts").getOrElse("")}"
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
         |  $tsCond
         |  group by s.pool_id
         |) as sx on sx.pool_id = p.pool_id
         |left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id
         |where sx.pool_id is not null
         """.query[PoolVolumeSnapshot]
  }

  def getPoolFees(tw: TimeWindow): Query0[PoolFeesSnapshot] = {
    val tsCond =
      Fragment.const(
        s"where ${tw.from.map(ts => s"s.timestamp >= $ts").getOrElse("")} ${if (tw.from.isDefined && tw.to.isDefined) "and"
        else ""} ${tw.to.map(ts => s"s.timestamp <= $ts").getOrElse("")}"
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
         |    cast(sum(case when (s.input_id = p.y_id) then s.output_amount::decimal * p.fee_num / 1000 else 0 end) as bigint) as tx,
         |    cast(sum(case when (s.input_id = p.x_id) then s.output_amount::decimal * p.fee_num / 1000 else 0 end) as bigint) as ty
         |  from swaps s
         |  left join pools p on p.pool_state_id = s.pool_state_id
         |  $tsCond
         |  group by s.pool_id
         |) as sx on sx.pool_id = p.pool_id
         |left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id
         |where sx.pool_id is not null
         """.query[PoolFeesSnapshot]
  }

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
         |    cast(sum(case when (s.input_id = p.y_id) then s.output_amount::decimal * p.fee_num / 1000 else 0 end) as bigint) as tx,
         |    cast(sum(case when (s.input_id = p.x_id) then s.output_amount::decimal * p.fee_num / 1000 else 0 end) as bigint) as ty
         |  from swaps s
         |  left join pools p on p.pool_state_id = s.pool_state_id
         |  where p.pool_id = $poolId $tsCond
         |  group by s.pool_id
         |) as sx on sx.pool_id = p.pool_id
         |left join assets ax on ax.id = p.x_id
         |left join assets ay on ay.id = p.y_id
         |where sx.pool_id is not null
         """.query[PoolFeesSnapshot]
  }
}
