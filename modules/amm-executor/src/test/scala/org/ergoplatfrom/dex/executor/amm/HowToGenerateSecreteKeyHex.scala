package org.ergoplatfrom.dex.executor.amm

import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.ergo.Address
import org.ergoplatform.wallet.mnemonic.Mnemonic
import org.ergoplatform.wallet.secrets.ExtendedSecretKey
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

  /**
    * How to generate a proper secrete key for AMM bots:
    *   1. Put your mnemonic key into a mnemonic variable like this:
    *      'val mnemonic: String = "this is my mnemonic key"'
    *      or let this script generate it for you. Save it somewhere to restore your wallet in the future!
    *   2. Run this script. Outputs are - mnemonic, address, secrete key hex
    *   3. Send some ergs to the address you get in stdout(With SPF fee orders, the off-chain operator now provides miner fee
    *      from his wallet but receives SPF fee equivalent for this miner fee).
    *   4. Put SKHex string into amm bots config here exchange.sk-hex = ""
    *   5. Run bots
    */

  val mnemonic: String      = new Mnemonic("english", 256).generate.get
  val seed: Array[Byte]     = Mnemonic.toSeed(mnemonic)
  val SK: ExtendedSecretKey = deriveMasterKey(seed)
  val address               = Address.fromStringUnsafe(addressEncoder.toString(P2PKAddress(SK.publicImage)))
  val SKHex                 = Base16.encode(SK.keyBytes)

  println("---------------------------")
  println(s"Mnemonic key is: $mnemonic")
  println("---------------------------")
  println(s"Address is: $address")
  println("---------------------------")
  println(s"Secrete key hex is: $SKHex")
  println("---------------------------")
}
