package org.ergoplatform.dex.protocol.amm

import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.T2TCFMM
import org.ergoplatform.ergo.SErgoTree
import sigmastate.Values.ErgoTree

trait AmmContracts[CT <: AMMType] {

  def pool: ErgoTree
}

object AmmContracts {

  implicit val T2TCFMMContracts: AmmContracts[T2TCFMM] =
    new AmmContracts[T2TCFMM] {

      def pool: ErgoTree =
        ErgoTreeSerializer.default.deserialize(
          SErgoTree.unsafeFromString(
            "19a5030f040004020402040404040406040605feffffffffffffffff0105feffffffffffffffff01050004d00f04000400050005" +
            "00d81ad601b2a5730000d602e4c6a70405d603db63087201d604db6308a7d605b27203730100d606b27204730200d607b2720373" +
            "0300d608b27204730400d609b27203730500d60ab27204730600d60b9973078c720602d60c999973088c720502720bd60d8c7208" +
            "02d60e998c720702720dd60f91720e7309d6108c720a02d6117e721006d6127e720e06d613998c7209027210d6147e720d06d615" +
            "730ad6167e721306d6177e720c06d6187e720b06d6199c72127218d61a9c72167218d1edededededed93c27201c2a793e4c67201" +
            "0405720292c17201c1a793b27203730b00b27204730c00938c7205018c720601ed938c7207018c720801938c7209018c720a0195" +
            "93720c730d95720f929c9c721172127e7202069c7ef07213069a9c72147e7215067e9c720e720206929c9c721472167e7202069c" +
            "7ef0720e069a9c72117e7215067e9c721372020695ed720f917213730e907217a19d721972149d721a7211ed9272199c72177214" +
            "92721a9c72177211"
          )
        )
    }
}
