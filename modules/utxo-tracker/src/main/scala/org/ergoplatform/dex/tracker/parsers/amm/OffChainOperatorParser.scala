package org.ergoplatform.dex.tracker.parsers.amm

import cats.Applicative
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.amm.{CFMMVersionedOrder, OffChainOperator}
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.dex.protocol.amm.constants._
import org.ergoplatform.ergo.ErgoTreeTemplate
import tofu.syntax.foption._
import cats.syntax.applicative._
import cats.syntax.option._

trait OffChainOperatorParser {
  def parse[A <: CFMMVersionedOrder.Any](o: A, output: Output): Option[OffChainOperator]
}

object OffChainOperatorParser {

  implicit def make(implicit e: ErgoAddressEncoder): OffChainOperatorParser = new Impl

  final class Impl(implicit e: ErgoAddressEncoder) extends OffChainOperatorParser {

    def parse[A <: CFMMVersionedOrder.Any](o: A, output: Output): Option[OffChainOperator] = {
      val tree     = ErgoTreeSerializer.default.deserialize(output.ergoTree)
      val template = ErgoTreeTemplate.fromBytes(tree.template)
      if (reservedErgoTrees.contains(template)) none
      else {
        val tree    = ErgoTreeSerializer.default.deserialize(output.ergoTree)
        val address = e.fromProposition(tree).toOption
        val fee     = output.value
        address.map(a => OffChainOperator(o.id, output.boxId, e.toString(a), fee))
      }

    }
  }
}
