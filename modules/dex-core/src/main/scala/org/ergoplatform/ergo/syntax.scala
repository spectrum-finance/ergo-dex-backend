package org.ergoplatform.ergo

import org.ergoplatform.{ErgoAddressEncoder, ErgoBox}
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.Values.{ByteArrayConstant, Constant, ErgoTree, SigmaPropConstant}
import sigmastate.basics.DLogProtocol
import sigmastate.basics.DLogProtocol.ProveDlogProp
import sigmastate.{SLong, SType, Values}
import special.collection.Coll
import sigmastate.eval.Extensions._

object syntax {

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

  implicit final class ConstantsOps(private val constants: IndexedSeq[Constant[SType]]) extends AnyVal {

    def parseLong(idx: Int): Option[Long] =
      constants.lift(idx).collect { case Values.ConstantNode(value, SLong) =>
        value.asInstanceOf[Long]
      }

    def parseBytea(idx: Int): Option[Array[Byte]] =
      constants.lift(idx).collect { case ByteArrayConstant(coll) => coll.toArray }

    def parsePk(idx: Int): Option[DLogProtocol.ProveDlog] =
      constants.lift(idx).collect { case SigmaPropConstant(ProveDlogProp(v)) => v }
  }
}
