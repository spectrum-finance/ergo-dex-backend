package org.ergoplatform.dex.markets.db.sql

import doobie.LogHandler
import doobie.implicits._
import doobie.util.query.Query0
import org.ergoplatform.dex.markets.db.models.{PoolSnapshot, PoolVolumeSnapshot}

class AnalyticsSql(implicit lg: LogHandler) {

  def getPoolSnapshots: Query0[PoolSnapshot] =
    sql"""
         |select p.pool_id, p.x_id, p.x_amount, p.x_ticker, p.y_id, p.y_amount, p.y_ticker
         |from pools p
         |left join (
         |	select pool_id, max(gindex) as gindex
         |	from pools
         |	group by pool_id
         |) as px on p.pool_id = px.pool_id and p.gindex = px.gindex
         |where px.gindex = p.gindex
         """.stripMargin.query[PoolSnapshot]

  def getPoolVolumes(fromTs: Long): Query0[PoolVolumeSnapshot] =
    sql"""
         |select distinct on (p.pool_id) p.pool_id, p.x_id, sx.tx, p.x_ticker, p.y_id, sx.ty, p.y_ticker from pools p
         |left join (
         |	select s.pool_id, sum(case when (s.input_id = p.y_id) then s.output_amount else 0 end) as tx, sum(case when (s.input_id = p.x_id) then s.output_amount else 0 end) as ty
         |	from swaps s
         |	left join pools p on p.pool_state_id = s.pool_state_id
         |  where s.timestamp >= $fromTs
         |	group by s.pool_id
         |) as sx on sx.pool_id = p.pool_id
         |where sx.pool_id is not null
         """.query[PoolVolumeSnapshot]
}
