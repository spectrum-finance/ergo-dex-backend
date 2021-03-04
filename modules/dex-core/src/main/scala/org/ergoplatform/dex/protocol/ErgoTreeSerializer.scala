package org.ergoplatform.dex.protocol

import org.ergoplatform.dex.ErgoTree
import sigmastate.Values
import sigmastate.serialization.{ErgoTreeSerializer => TreeSerializeer}

trait ErgoTreeSerializer {

  def serialize(tree: sigmastate.Values.ErgoTree): ErgoTree

  def deserialize(raw: ErgoTree): sigmastate.Values.ErgoTree
}

object ErgoTreeSerializer {

  def default: ErgoTreeSerializer =
    new ErgoTreeSerializer {

      def serialize(tree: Values.ErgoTree): ErgoTree =
        ErgoTree.fromBytes(TreeSerializeer.DefaultSerializer.serializeErgoTree(tree))

      def deserialize(raw: ErgoTree): Values.ErgoTree =
        TreeSerializeer.DefaultSerializer.deserializeErgoTree(raw.toBytea)
    }
}
