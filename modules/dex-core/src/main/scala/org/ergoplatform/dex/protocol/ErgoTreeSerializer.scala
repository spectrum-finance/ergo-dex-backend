package org.ergoplatform.dex.protocol

import org.ergoplatform.dex.SErgoTree
import sigmastate.Values
import sigmastate.serialization.{ErgoTreeSerializer => TreeSerializer}

trait ErgoTreeSerializer {

  def serialize(tree: sigmastate.Values.ErgoTree): SErgoTree

  def deserialize(raw: SErgoTree): sigmastate.Values.ErgoTree
}

object ErgoTreeSerializer {

  object default extends ErgoTreeSerializer {

      def serialize(tree: Values.ErgoTree): SErgoTree =
        SErgoTree.fromBytes(TreeSerializer.DefaultSerializer.serializeErgoTree(tree))

      def deserialize(raw: SErgoTree): Values.ErgoTree =
        TreeSerializer.DefaultSerializer.deserializeErgoTree(raw.toBytea)
    }
}
