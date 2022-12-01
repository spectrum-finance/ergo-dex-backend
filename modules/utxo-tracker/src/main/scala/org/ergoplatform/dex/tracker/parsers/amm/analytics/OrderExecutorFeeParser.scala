package org.ergoplatform.dex.tracker.parsers.amm.analytics

import cats.syntax.option._
import org.ergoplatform.dex.domain.amm.{CFMMPool, CFMMVersionedOrder, OrderExecutorFee}
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.constants._
import org.ergoplatform.ergo.ErgoTreeTemplate
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}

trait OrderExecutorFeeParser {

  def parse[A <: CFMMVersionedOrder.Any](
    o: A,
    output: Output,
    ts: Long,
    pool: CFMMPool
  ): Option[OrderExecutorFee]
}

object OrderExecutorFeeParser {

  implicit def make(implicit e: ErgoAddressEncoder): OrderExecutorFeeParser = new Impl

  final class Impl(implicit e: ErgoAddressEncoder) extends OrderExecutorFeeParser {

    def parse[A <: CFMMVersionedOrder.Any](
      o: A,
      output: Output,
      ts: Long,
      pool: CFMMPool
    ): Option[OrderExecutorFee] = {
      val (rewardPubKey, redeemerTree) = o match {
        case swap: CFMMVersionedOrder.SwapV0       => swap.params.redeemer.some    -> none
        case swap: CFMMVersionedOrder.SwapV1       => swap.params.redeemer.some    -> none
        case swap: CFMMVersionedOrder.SwapV2       => none                         -> swap.params.redeemer.some
        case swap: CFMMVersionedOrder.SwapV3       => none                         -> swap.params.redeemer.some
        case deposit: CFMMVersionedOrder.DepositV0 => deposit.params.redeemer.some -> none
        case deposit: CFMMVersionedOrder.DepositV1 => deposit.params.redeemer.some -> none
        case deposit: CFMMVersionedOrder.DepositV2 => deposit.params.redeemer.some -> none
        case deposit: CFMMVersionedOrder.DepositV3 => none                         -> deposit.params.redeemer.some
        case redeem: CFMMVersionedOrder.RedeemV0   => redeem.params.redeemer.some  -> none
        case redeem: CFMMVersionedOrder.RedeemV1   => redeem.params.redeemer.some  -> none
        case redeem: CFMMVersionedOrder.RedeemV3   => none                         -> redeem.params.redeemer.some
      }

      val tree     = ErgoTreeSerializer.default.deserialize(output.ergoTree)
      val template = ErgoTreeTemplate.fromBytes(tree.template)
      val address  = e.fromProposition(tree).toOption

      val rewardAddress = rewardPubKey.flatMap { key =>
        val treePubKey = ErgoTreeSerializer.default.deserialize(key.ergoTree)
        e.fromProposition(treePubKey).toOption.map(e.toString)
      }

      def isP2PK = address.exists {
        case _: P2PKAddress => true
        case _              => false
      }

      def isTheSameErgTree = redeemerTree.contains(output.ergoTree)

      def matchAddresses =
        (for {
          aOut <- address.map(e.toString)
          aOrd <- rewardAddress
        } yield aOut == aOrd).getOrElse(false)

      if (
        reservedErgoTrees.contains(template) || output.assets.nonEmpty || matchAddresses || !isP2PK || isTheSameErgTree
      )
        none
      else
        address.map(e.toString).map(a => OrderExecutorFee(pool.poolId, o.id, output.boxId, a, output.value, ts))
    }
  }
}
