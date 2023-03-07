package org.ergoplatfrom.dex.executor.amm

import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.ergo.Address
import org.ergoplatform.wallet.mnemonic.Mnemonic
import org.ergoplatform.wallet.secrets.{DerivationPath, ExtendedSecretKey}
import org.ergoplatform.wallet.secrets.ExtendedSecretKey.deriveMasterKey
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scorex.util.encode.Base16

final class HowToGenerateSecreteKeyHex
  extends AnyPropSpec
  with should.Matchers
  with ScalaCheckPropertyChecks
  with CatsPlatform {

  implicit val addressEncoder: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  /** How to generate a proper mnemonic for AMM bots:
    *   1. If you have your key, e.g., generated by your wallet,
    *      put it into the config file. Otherwise, generate it via this script.
    *   2. Ensure you have funds on the same address bots will use.
    *      To check it, put your mnemonic in the `mnemonic` variable (val mnemonic: String = "YOUR MNEMONIC"),
    *      run the script and check the address stdout.
    */

  val mnemonic: String      = new Mnemonic("english", 256).generate.get
  val seed: Array[Byte]     = Mnemonic.toSeed(mnemonic)
  val SK: ExtendedSecretKey = deriveMasterKey(seed)
  val path                  = "m/44'/429'/0'/0/0"
  val derivationPath        = DerivationPath.fromEncoded(path).get
  val nextSK                = SK.derive(derivationPath).asInstanceOf[ExtendedSecretKey]
  val address               = Address.fromStringUnsafe(addressEncoder.toString(P2PKAddress(nextSK.publicImage)))
  val SKHex                 = Base16.encode(nextSK.keyBytes)

  println("---------------------------")
  println(s"Mnemonic key is: $mnemonic")
  println("---------------------------")
  println(s"Address is: $address")
  println("---------------------------")
  println(s"Secrete key hex is: $SKHex")
  println("---------------------------")
}
