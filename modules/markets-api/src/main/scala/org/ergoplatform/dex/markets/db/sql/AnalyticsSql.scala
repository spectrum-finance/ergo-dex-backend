package org.ergoplatform.dex.markets.db.sql

import doobie.implicits._
import doobie.util.query.Query0
import doobie.{Fragment, Fragments, LogHandler}
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.markets.api.v1.models.amm.Order.{AnyOrder, Deposit, Lock, Redeem, Swap}
import org.ergoplatform.dex.markets.api.v1.models.amm.{Order, OrderStatus}
import org.ergoplatform.dex.markets.db.models.amm._
import org.ergoplatform.ergo.{Address, TokenId, TxId}
import cats.syntax.list._
import cats.syntax.traverse._
import cats.instances.option._

final class AnalyticsSql(implicit lg: LogHandler) {

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

  def getAllOrders(
    addresses: List[Address],
    offset: Int,
    limit: Int,
    tw: TimeWindow,
    status: Option[OrderStatus],
    assetId: Option[TokenId],
    txId: Option[TxId]
  ): Query0[AnyOrder] =
    sql"""
         |select s.input_id, s.input_value, s.min_output_id, s.output_amount, null, null, null, null, null, s.timestamp,
         |s.register_transaction_id, s.pool_id, s.output_amount * (dex_fee_per_token_num / dex_fee_per_token_denom),
         |s.executed_transaction_id, s.status from swaps s
         |${orderCond(addresses, status, txId, assetId.map(tokenId => fr"s.input_id = $tokenId"), optTimeWindowCond(tw, "s"))}
         |union
         |select s.input_id_x, s.input_amount_x, s.input_id_y, s.input_amount_y, output_id_lp, output_amount_lp, null, null, null, s.timestamp, s.register_transaction_id,
         |s.pool_id, dex_fee, s.executed_transaction_id, s.status from deposits s
         |${orderCond(addresses, status, txId, assetId.map(tokenId => fr"s.input_id_x = $tokenId OR s.input_id_y = $tokenId"), optTimeWindowCond(tw, "s"))}
         |union
         |select s.output_id_x, s.output_amount_x, s.output_id_y, s.output_amount_y, s.lp_id, s.lp_amount, null, null, null, s.timestamp, s.register_transaction_id, s.pool_id,
         |dex_fee, s.executed_transaction_id, s.status from redeems s
         |${orderCond(addresses, status, txId, assetId.map(tokenId => fr"s.lp_id = $tokenId"), optTimeWindowCond(tw, "s"))}
         |union
         |select null, null, null, null, null, null, s.token_id, s.amount::text, s.deadline::text, s.timestamp, s.register_transaction_id,
         |null, null s.status from lq_locks s
         |${orderCond(addresses, status, txId, assetId.map(tokenId => fr"s.token_id = $tokenId"), optTimeWindowCond(tw, "s"))}
         |offset $offset limit $limit
         """.stripMargin.query[AnyOrder]

  def getSwaps(
    addresses: List[Address],
    offset: Int,
    limit: Int,
    tw: TimeWindow,
    status: Option[OrderStatus],
    assetId: Option[TokenId],
    txId: Option[TxId]
  ): Query0[Swap] =
    sql"""
         |select s.input_id, s.input_value, s.timestamp, s.register_transaction_id, s.pool_id,
         |s.output_amount * (dex_fee_per_token_num / dex_fee_per_token_denom),
         |s.min_output_id, s.output_amount, s.executed_transaction_id, s.status from swaps s
         |${orderCond(addresses, status, txId, assetId.map(tokenId => fr"s.input_id = $tokenId"), optTimeWindowCond(tw, "s"))}
         |offset $offset limit $limit
         """.stripMargin.query[Swap]

  def getDeposits(
    addresses: List[Address],
    offset: Int,
    limit: Int,
    tw: TimeWindow,
    status: Option[OrderStatus],
    assetId: Option[TokenId],
    txId: Option[TxId]
  ): Query0[Deposit] =
    sql"""
       |select s.input_id_x, s.input_amount_x, s.input_id_y, s.input_amount_y, s.timestamp, s.register_transaction_id,
       |s.pool_id, dex_fee, output_id_lp, output_amount_lp, s.executed_transaction_id, s.status from deposits s
       |${orderCond(addresses, status, txId, assetId.map(tokenId => fr"s.input_id_x = $tokenId OR s.input_id_y = $tokenId"), optTimeWindowCond(tw, "s"))}
       |offset $offset limit $limit
       """.stripMargin.query[Deposit]

  def getRedeems(
    addresses: List[Address],
    offset: Int,
    limit: Int,
    tw: TimeWindow,
    status: Option[OrderStatus],
    assetId: Option[TokenId],
    txId: Option[TxId]
  ): Query0[Redeem] =
    sql"""
       |select s.lp_id, s.lp_amount, s.timestamp, s.register_transaction_id, s.pool_id,
       |dex_fee, s.output_id_x, s.output_amount_x, s.output_id_y, s.output_amount_y, s.executed_transaction_id, s.status from redeems s
       |${orderCond(addresses, status, txId, assetId.map(tokenId => fr"s.lp_id = $tokenId"), optTimeWindowCond(tw, "s"))}
       |offset $offset limit $limit
       """.stripMargin.query[Redeem]

  def getLocks(
    addresses: List[Address],
    offset: Int,
    limit: Int,
    tw: TimeWindow,
    status: Option[OrderStatus],
    assetId: Option[TokenId],
    txId: Option[TxId]
  ): Query0[Lock] =
    sql"""
       |select s.token_id, s.amount, s.timestamp, s.register_transaction_id, s.deadline, null, s.status from lq_locks s
       |${orderCond(addresses, status, txId, assetId.map(tokenId => fr"s.token_id = $tokenId"), optTimeWindowCond(tw, "s"))}
       |offset $offset limit $limit
       """.stripMargin.query[Lock]

  private def orderCond(
    addresses: List[Address],
    status: Option[OrderStatus],
    txId: Option[TxId],
    assetId: Option[Fragment],
    tw: Option[Fragment]
  ) =
    Fragments.whereAndOpt(
      addresses.toNel.map(Fragments.in(fr"s.redeemer", _)),
      status.map(st => fr"s.status = $st"),
      txId.map(id => fr"s.register_transaction_id = $id"),
      assetId,
      tw
    )

  private def timeWindowCond(tw: TimeWindow, condKeyword: String, alias: String): Fragment =
    if (tw.from.nonEmpty || tw.to.nonEmpty)
      Fragment.const(
        s"$condKeyword ${tw.from.map(ts => s"$alias.timestamp >= $ts").getOrElse("")} ${if (tw.from.isDefined && tw.to.isDefined) "and"
        else ""} ${tw.to.map(ts => s"$alias.timestamp <= $ts").getOrElse("")}"
      )
    else Fragment.empty

  private def optTimeWindowCond(tw: TimeWindow, alias: String): Option[Fragment] =
    if (tw.from.nonEmpty || tw.to.nonEmpty)
      Some(
        Fragment.const(
          s"${tw.from.map(ts => s"$alias.timestamp >= $ts").getOrElse("")} ${if (tw.from.isDefined && tw.to.isDefined) "and"
          else ""} ${tw.to.map(ts => s"$alias.timestamp <= $ts").getOrElse("")}"
        )
      )
    else None

  private def blockTimeWindowMapping(tw: TimeWindow): Fragment =
    if (tw.from.nonEmpty || tw.to.nonEmpty)
      Fragment.const(
        s"${tw.from.map(ts => s"b.timestamp >= $ts").getOrElse("")} ${if (tw.from.isDefined && tw.to.isDefined) "and"
        else ""} ${tw.to.map(ts => s"b.timestamp <= $ts").getOrElse("")}"
      )
    else Fragment.const("true")
}
