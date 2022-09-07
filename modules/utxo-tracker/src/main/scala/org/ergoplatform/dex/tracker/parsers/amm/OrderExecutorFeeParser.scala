package org.ergoplatform.dex.tracker.parsers.amm

import cats.syntax.option._
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import org.ergoplatform.dex.domain.amm.{CFMMPool, CFMMVersionedOrder, OrderExecutorFee}
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.constants._
import org.ergoplatform.ergo.{ErgoTreeTemplate, PubKey}
import org.ergoplatform.ergo.domain.Output

trait OrderExecutorFeeParser {

  def parse[A <: CFMMVersionedOrder.Any](
    o: A,
    output: Output,
    ts: Long,
    pool: Option[CFMMPool]
  ): Option[OrderExecutorFee]
}

object OrderExecutorFeeParser {

  implicit def make(implicit e: ErgoAddressEncoder): OrderExecutorFeeParser = new Impl

  final class Impl(implicit e: ErgoAddressEncoder) extends OrderExecutorFeeParser {

    def parse[A <: CFMMVersionedOrder.Any](
      o: A,
      output: Output,
      ts: Long,
      pool: Option[CFMMPool]
    ): Option[OrderExecutorFee] = {
      val rewardPubKey = o match {
        case swap: CFMMVersionedOrder.SwapV0       => swap.params.redeemer
        case swap: CFMMVersionedOrder.SwapV1       => swap.params.redeemer
        case deposit: CFMMVersionedOrder.DepositV0 => deposit.params.redeemer
        case deposit: CFMMVersionedOrder.DepositV1 => deposit.params.redeemer
        case redeem: CFMMVersionedOrder.RedeemV0   => redeem.params.redeemer
        case redeem: CFMMVersionedOrder.RedeemV1   => redeem.params.redeemer
      }

      val tree     = ErgoTreeSerializer.default.deserialize(output.ergoTree)
      val template = ErgoTreeTemplate.fromBytes(tree.template)
      val address  = e.fromProposition(tree).toOption

      val treePubKey    = ErgoTreeSerializer.default.deserialize(rewardPubKey.ergoTree)
      val rewardAddress = e.fromProposition(treePubKey).toOption.map(e.toString)

      def isP2PK = address.exists {
        case _: P2PKAddress => true
        case _              => false
      }

      def matchAddresses =
        (for {
          aOut <- address.map(e.toString)
          aOrd <- rewardAddress
        } yield aOut == aOrd).getOrElse(false)

      if (reservedErgoTrees.contains(template) || output.assets.nonEmpty || matchAddresses || !isP2PK)
        none
      else
        address.map(e.toString).map(a => OrderExecutorFee(pool.map(_.poolId), o.id, output.boxId, a, output.value, ts))
    }
  }
}
