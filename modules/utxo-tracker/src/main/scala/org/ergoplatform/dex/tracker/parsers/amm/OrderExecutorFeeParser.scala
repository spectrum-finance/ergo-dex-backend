package org.ergoplatform.dex.tracker.parsers.amm

import cats.syntax.option._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.amm.{CFMMVersionedOrder, OrderExecutorFee}
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.constants._
import org.ergoplatform.ergo.{ErgoTreeTemplate, PubKey}
import org.ergoplatform.ergo.domain.Output

trait OrderExecutorFeeParser {
  def parse[A <: CFMMVersionedOrder.Any](o: A, output: Output, ts: Long): Option[OrderExecutorFee]
}

object OrderExecutorFeeParser {

  implicit def make(implicit e: ErgoAddressEncoder): OrderExecutorFeeParser = new Impl

  final class Impl(implicit e: ErgoAddressEncoder) extends OrderExecutorFeeParser {

    def parse[A <: CFMMVersionedOrder.Any](o: A, output: Output, ts: Long): Option[OrderExecutorFee] = {
      val tree     = ErgoTreeSerializer.default.deserialize(output.ergoTree)
      val template = ErgoTreeTemplate.fromBytes(tree.template)
      if (reservedErgoTrees.contains(template)) none
      else {
        val tree    = ErgoTreeSerializer.default.deserialize(output.ergoTree)
        val address = e.fromProposition(tree).toOption
        val fee     = output.value
        address.map(a => OrderExecutorFee(o.id, output.boxId, PubKey.fromBytes(e.toString(a).getBytes), fee, ts))
      }
    }
  }
}
