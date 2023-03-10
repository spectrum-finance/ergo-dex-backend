package org.ergoplatform.ergo

import org.bouncycastle.util.BigIntegers
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import org.ergoplatform.wallet.mnemonic.Mnemonic
import org.ergoplatform.wallet.secrets.{DerivationPath, ExtendedSecretKey}
import org.ergoplatform.wallet.secrets.ExtendedSecretKey.deriveMasterKey
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol.DLogProverInput

object PrivKeyGenerator {

  def make(mnemonic: String)(implicit e: ErgoAddressEncoder): (DLogProverInput, Address) = {
    val seed: Array[Byte]     = Mnemonic.toSeed(mnemonic)
    val SK: ExtendedSecretKey = deriveMasterKey(seed)
    val path                  = "m/44'/429'/0'/0/0"
    val derivationPath        = DerivationPath.fromEncoded(path).get
    val nextSK                = SK.derive(derivationPath).asInstanceOf[ExtendedSecretKey]
    val address               = Address.fromStringUnsafe(e.toString(P2PKAddress(nextSK.publicImage)))
    val SKHex                 = Base16.encode(nextSK.keyBytes)
    val sk: DLogProverInput   = DLogProverInput(BigIntegers.fromUnsignedByteArray(Base16.decode(SKHex).get))

    (sk, address)
  }

}
