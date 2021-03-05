package org.ergoplatform.dex.protocol

import org.ergoplatform.dex.SErgoTree
import sigmastate.Values
import sigmastate.serialization.{ErgoTreeSerializer => TreeSerializeer}

trait ErgoTreeSerializer {

  def serialize(tree: sigmastate.Values.ErgoTree): SErgoTree

  def deserialize(raw: SErgoTree): sigmastate.Values.ErgoTree
}

object ErgoTreeSerializer {

  def default: ErgoTreeSerializer =
    new ErgoTreeSerializer {

      def serialize(tree: Values.ErgoTree): SErgoTree =
        SErgoTree.fromBytes(TreeSerializeer.DefaultSerializer.serializeErgoTree(tree))

      def deserialize(raw: SErgoTree): Values.ErgoTree =
        TreeSerializeer.DefaultSerializer.deserializeErgoTree(raw.toBytea)
    }
}
