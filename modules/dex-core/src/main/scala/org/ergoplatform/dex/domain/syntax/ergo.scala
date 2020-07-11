package org.ergoplatform.dex.domain.syntax

import org.ergoplatform.ErgoBox
import org.ergoplatform.dex.{Address, AssetId, BoxId}
import sigmastate.Values.ErgoTree
import special.collection.Coll

object ergo {

  implicit final class BoxIdOps(private val id: BoxId) extends AnyVal {
    def toErgo: ErgoBox.BoxId = ???
    def toSigma: Coll[Byte] = ???
  }

  implicit final class AssetIdOps(private val id: AssetId) extends AnyVal {
    def toErgo: ErgoBox.TokenId = ???
  }

  implicit final class AddressOps(private val address: Address) extends AnyVal {
    def toErgoTree: ErgoTree= ???
  }
}
