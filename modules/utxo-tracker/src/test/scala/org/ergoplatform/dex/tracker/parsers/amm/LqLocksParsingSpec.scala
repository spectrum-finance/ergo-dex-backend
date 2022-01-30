package org.ergoplatform.dex.tracker.parsers.amm

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.tracker.parsers.locks.LiquidityLockParser
import org.ergoplatform.ergo.models.Output
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class LqLocksParsingSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with CatsPlatform {

  implicit val e: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  property("Valid Liquidity Lock parsing") {
    val p      = LiquidityLockParser.locksV0
    val parsed = p.parse(outSample)
    parsed.isDefined shouldBe true
  }

  def outSample =
    io.circe.parser
      .decode[Output]("""
          |{
          |    "boxId": "553699ea402eab6d7bf7547ac796d2d06146a454abe83276e11718efebac7eca",
          |    "transactionId": "c6d6f462271d66c302a3abbf2a5a4c17b1d4444296a8fb42642a63d3c3ab8e88",
          |    "blockId": "1416e77ed28e46bec7f044e6bfcebefa8ea41fce507b9cc3221f7e0b3d0cb8e4",
          |    "value": 2060000,
          |    "index": 0,
          |    "globalIndex": 12366818,
          |    "creationHeight": 671442,
          |    "settlementHeight": 671446,
          |    "ergoTree": "195e03040004000400d802d601b2a5730000d602e4c6a70404ea02e4c6a70508d19593c27201c2a7d802d603b2db63087201730100d604b2db6308a7730200eded92e4c6720104047202938c7203018c720401928c7203028c7204028f7202a3",
          |    "address": "XqM6yyAmxNgCcRzvutWwtdSvKqqaEtd4cZRsVvJu1xeu4y5T9tZexJpPf1XMMWCZdv8zVK1XUbmM5gjB9KzWXQCMEWJcdNas6HYJFYf47m63kU3xZMHjUA3vNKRZWEj8AvQ75YBUx",
          |    "assets": [
          |        {
          |            "tokenId": "303f39026572bcb4060b51fafc93787a236bb243744babaa99fceb833d61e198",
          |            "index": 0,
          |            "amount": 59642,
          |            "name": null,
          |            "decimals": null,
          |            "type": null
          |        }
          |    ],
          |    "additionalRegisters": {
          |        "R4": {
          |            "serializedValue": "04c48652",
          |            "sigmaType": "SInt",
          |            "renderedValue": "672162"
          |        },
          |        "R5": {
          |            "serializedValue": "08cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec",
          |            "sigmaType": "SSigmaProp",
          |            "renderedValue": "03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec"
          |        }
          |    },
          |    "spentTransactionId": null,
          |    "mainChain": true
          |}
          |""".stripMargin)
      .toOption
      .get
}
