package org.ergoplatform.dex.protocol

import sigmastate.Values.ErgoTree

object sigmaUtils {

  @inline def updateVersionHeader(tree: ErgoTree): ErgoTree = {
    val versionHeader = ErgoTree.headerWithVersion(version = 1)
    val header =
      if (ErgoTree.isConstantSegregation(tree.header)) (versionHeader | ErgoTree.ConstantSegregationFlag).toByte
      else versionHeader
    tree.copy(header = header)
  }
}
