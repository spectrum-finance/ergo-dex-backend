package org.ergoplatform.dex.domain.syntax

import org.ergoplatform.dex.domain.network.Output
import org.ergoplatform.dex.{Address, BoxId, TokenId}
import org.ergoplatform.{ErgoAddressEncoder, ErgoBox, Input}
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.Values.ErgoTree
import sigmastate.eval.Extensions._
import special.collection.Coll

object ergo {

  implicit final class BoxIdOps(private val id: BoxId) extends AnyVal {
    def toErgo: ErgoBox.BoxId = ADKey @@ Base16.decode(id.value).get
    def toSigma: Coll[Byte]   = toErgo.toColl
  }

  implicit final class AssetIdOps(private val id: TokenId) extends AnyVal {
    def toErgo: ErgoBox.TokenId = Digest32 @@ scorex.util.encode.Base16.decode(id.unwrapped).get
    def toSigma: Coll[Byte]     = toErgo.toColl
  }

  implicit final class AddressOps(private val address: Address) extends AnyVal {
    def toErgoTree(implicit e: ErgoAddressEncoder): ErgoTree = e.fromString(address.unwrapped).get.script
  }
}
