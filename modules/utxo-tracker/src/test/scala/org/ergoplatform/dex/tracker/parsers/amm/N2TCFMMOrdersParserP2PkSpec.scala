package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.IO
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo._
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class N2TCFMMOrdersParserP2PkSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with CatsPlatform {

  property("N2T Deposit order parsing") {
    val res = parser.deposit(boxSample).unsafeRunSync()
    println(res)
  }

  implicit val e: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)
  def parser                         = N2TCFMMOrdersParserP2Pk.make[IO]

  def boxSample =
    io.circe.parser
      .decode[Output](
        """
           |{
           |    "boxId": "f5306ddbf604760db3f0b36f7cc59c860f12d512d4be0284f618e1d5546ee6d4",
           |    "transactionId": "bd9a2172f65a05512d24fdf62bfc3b042299cf43c9549e268f9cf34c91e67130",
           |    "blockId": "491f9dc50cdf143f789236af7e98beefaad4dca7647d500050d09e465d88aee7",
           |    "value": 1020000000,
           |    "index": 0,
           |    "globalIndex": 6680469,
           |    "creationHeight": 508928,
           |    "settlementHeight": 564625,
           |    "ergoTree": "199d021008cd02c3f56e66191a903758f53a4b90d07cef80f93e7a4f17d106098ad0caf189722a04000404040604020400040205feffffffffffffffff0104000e20bee300e9c81e48d7ab5fc29294c7bbb536cf9dcd9c91ee3be9898faec91b11b60580dac40905f0c06c06030d9038040004040100d802d6017300d602b2a4730100eb027201d195ed93b1a4730293b1db630872027303d805d603db63087202d604b2a5730400d605b2db63087204730500d606b27203730600d6077e9973078c72060206edededed938cb2720373080001730993c27204d0720192c172049999c1a7730a730b938c7205018c720601927e8c72050206a19d9c730c72077ec17202069d9c7e8cb2db6308a7730d00020672077e8cb27203730e000206730f",
           |    "address": "2EJUZ6E3m4uuTLaJZGQ4vwHYpvyn9DcShLtwhVZXiSC5v4v6LuypZZsRgRnq3CmqUH1Do5QTfngiPXVmYmcUGAhzQnhThZx4VSVLZqViewtz7oD8gDb4SwBv23z3FLJ5EERsAgsRLk6fCWqdoWm9eRqXAccnjR7AFNn1pREkWTLDMHRGWpgtA7RPtFUWzRCBBR5Kd1qBGNMpzU533Pqdm1qEr7fkYMdbDVePTjwZ3ikNbSM4GgfRnW7ubV7PHAHjmPH5r2ZTKMP1PvjhXYPg8wRH8guWfVQiKKCVokvS9KuVHtcMKUAkbgZQwRjQ6dMNKSW3UFNNc7sLXank8YVcqoSv9opqKsKfuZDNU5yUEFdCmAToKoJvKkD67y2EMCm1J4KTu4wEa41oGokt",
           |    "assets": [
           |        {
           |            "tokenId": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
           |            "index": 0,
           |            "amount": 1980,
           |            "name": "SigUSD",
           |            "decimals": 2,
           |            "type": "EIP-004"
           |        }
           |    ],
           |    "additionalRegisters": {},
           |    "spentTransactionId": null,
           |    "mainChain": true
           |}
           |""".stripMargin
      )
      .toOption
      .get
}
