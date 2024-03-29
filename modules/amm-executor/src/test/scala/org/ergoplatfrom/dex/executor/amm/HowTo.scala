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

final class HowTo
  extends AnyPropSpec
  with should.Matchers
  with ScalaCheckPropertyChecks
  with CatsPlatform {

  implicit val addressEncoder: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  /** How to generate a proper seed phrase for AMM bots.
    *
    *   Seed phrase is the same as mnemonic key.
    *   There is no way to generate seed phrase from your address.
    *   There is no way to use address you wanted if you don't have seed phrase for it.
    *   You can only use the address generated by the bots from the seed phrase you provided using eip3.
    *   Save your seed phrase for every wallet you care about!!!
    *   It will be safer and better for you not to use your regular wallet but create a new one for bots.
    *   Bots can take funds only from the first address, generated from provided seed phrase using eip3 standard.
    *   It's crucial to have exactly 1 (one) box on address with all funds(ERGs for miner fees), otherwise, bots behaviour may be unpredictable.
    *
    *   If you don't have your seed phrase:
    *     1. Generate a new unique seed phrase via wallet (e.g. nautilus - just create a new wallet and save seed phrase)
    *        or run(section `How to run script`) this script and check line `Seed phrase is:` and save seed phrase (24 words) from stdout.
    *     2. SAVE YOUR SEED PHRASE SOMEWHERE. IF YOU WILL LOSE IT, YOU WON'T BE ABLE TO RESTORE IT FROM ADDRESS.
    *     3. Deposit funds on exactly the address, you see in `Address is:` line in stdout.
    *     4. Put your seed phrase into config file and run bots.
    *
    *
    *   If you already have your seed phrase key (CAUTION: DON'T USE YOUR REGULAR HOT WALLET, ALWAYS GENERATE NEW ONE FOR BOTS):
    *     1. Ensure you have funds on the same address bot will use (bots will always take exactly the first address from your wallet, generated using eip3)
    *        To check it, put your seed phrase in the `seedPhrase` variable (val seedPhrase: String = "YOUR SEED PHRASE"),
    *        run the script and check the address stdout. If address, printed by script, is the same, as address you keep required funds on,
    *        you are allowed to use this seed phrase for bots, otherwise, move your funds to required address (from script stdout).
    *     2. Put your seed phrase into config file and run bots.
    *
    *
    *   -- How to run script:
    *     1. Install git: `https://git-scm.com/`
    *     2. Install java (e.g. for mac os `brew install openjdk@11` or `https://www.java.com/en/download/help/linux_x64_install.html`)
    *     3. Install sbt: `https://www.scala-sbt.org/download.html`
    *     4. Clone repo: `git clone https://github.com/spectrum-finance/ergo-dex-backend.git`
    *     5. Move from cli into this repo (e.g. cd path_to_folder/ergo-dex-backend/ in linux or just open terminal in cloned folder)
    *        and type sbt "; project amm-executor; test" into CLI.
    *
    */

  val seedPhrase: String      = new Mnemonic("english", 256).generate.get
  val seed: Array[Byte]     = Mnemonic.toSeed(seedPhrase)
  val SK: ExtendedSecretKey = deriveMasterKey(seed)
  val path                  = "m/44'/429'/0'/0/0"
  val derivationPath        = DerivationPath.fromEncoded(path).get
  val nextSK                = SK.derive(derivationPath).asInstanceOf[ExtendedSecretKey]
  val address               = Address.fromStringUnsafe(addressEncoder.toString(P2PKAddress(nextSK.publicImage)))
  val SKHex                 = Base16.encode(nextSK.keyBytes)

  println("---------------------------")
  println(s"Seed phrase is: $seedPhrase")
  println("---------------------------")
  println(s"Address is: $address")
  println("---------------------------")
  println(s"Secrete key hex is: $SKHex")
  println("---------------------------")
}
