package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.IO
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMOrder.SwapP2Pk
import org.ergoplatform.dex.domain.amm.{PoolId, SwapParams}
import org.ergoplatform.dex.tracker.parsers.amm.v1.T2TOrdersV1Parser
import org.ergoplatform.ergo.domain.{BoxAsset, Output}
import org.ergoplatform.ergo._
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class T2TCFMMOrdersParserP2PkSpec
  extends AnyPropSpec
  with should.Matchers
  with ScalaCheckPropertyChecks
  with CatsPlatform {

  implicit val e: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  property("Swap order parsing") {
    val address = PubKey.unsafeFromString("025e690b6094c8ca62a61aa59a549c3b48b7631928db8ff32345bead80f9247a5b")
    val res = parser.swap(boxSample).unsafeRunSync()
    res shouldBe Some(
      SwapP2Pk(
        PoolId.fromStringUnsafe("bfb483069f2809349a9378e8a746cbdcb44c66f94d0e839fd14158e8829eea68"),
        2000000,
        1628107666776L,
        SwapParams(
          AssetAmount(
            TokenId.fromStringUnsafe("03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04"),
            100
          ),
          AssetAmount(
            TokenId.fromStringUnsafe("472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e8"),
            139670010
          ),
          dexFeePerTokenNum   = 4295839887174061L,
          dexFeePerTokenDenom = 100000000000000000L,
          redeemer            = address
        ),
        Output(
          BoxId("cd29cc599a8a53d28504f2752fc075286359d6b7f1e82582b7e6cd45468a6b29"),
          TxId("52cdef65d1a93e29774904b5457577e7ec351a6fded878f5ee63733020865495"),
          7260000,
          0,
          825489,
          SErgoTree.unsafeFromString(
            "19bc041708cd025e690b6094c8ca62a61aa59a549c3b48b7631928db8ff32345bead80f9247a5b04000e20472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e804c80f04d00f040404080402040004040400040606010104000e20bfb483069f2809349a9378e8a746cbdcb44c66f94d0e839fd14158e8829eea6805f4c799850105daa6e5a7e5c2a10f058080d0d88bdea2e3020e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730405000500058092f4010100d805d6017300d602b2a4730100d6037302d6047303d6057304eb027201d195ed93b1a4730593b1db630872027306d80ad606db63087202d607b2a5730700d608b2db63087207730800d6098c720802d60a7e720906d60bb27206730900d60c7e8c720b0206d60d7e8cb2db6308a7730a000206d60e7e8cb27206730b000206d60f9a720a730cedededededed938cb27206730d0001730e93c27207d07201938c7208017203927209730f927ec1720706997ec1a7069d9c720a7e7310067e73110695938c720b017203909c9c720c720d7e7204069c720f9a9c720e7e7205069c720d7e720406909c9c720e720d7e7204069c720f9a9c720c7e7205069c720d7e72040690b0ada5d90110639593c272107312c1721073137314d90110599a8c7210018c72100273157316"
          ),
          List(
            BoxAsset(
              TokenId.fromStringUnsafe("03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04"),
              100
            )
          ),
          Map()
        )
      )
    )

  }

  def parser = T2TOrdersV1Parser.make[IO]

  def boxSample =
    io.circe.parser
      .decode[Output](
        """
           |{
           |    "boxId": "cd29cc599a8a53d28504f2752fc075286359d6b7f1e82582b7e6cd45468a6b29",
           |    "transactionId": "52cdef65d1a93e29774904b5457577e7ec351a6fded878f5ee63733020865495",
           |    "blockId": "274b42d256323aa61a1735f55ca15071a789704b9c66d5daba19151f4199e7e3",
           |    "value": 7260000,
           |    "index": 0,
           |    "globalIndex": 20758110,
           |    "creationHeight": 825489,
           |    "settlementHeight": 825491,
           |    "ergoTree": "19bc041708cd025e690b6094c8ca62a61aa59a549c3b48b7631928db8ff32345bead80f9247a5b04000e20472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e804c80f04d00f040404080402040004040400040606010104000e20bfb483069f2809349a9378e8a746cbdcb44c66f94d0e839fd14158e8829eea6805f4c799850105daa6e5a7e5c2a10f058080d0d88bdea2e3020e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730405000500058092f4010100d805d6017300d602b2a4730100d6037302d6047303d6057304eb027201d195ed93b1a4730593b1db630872027306d80ad606db63087202d607b2a5730700d608b2db63087207730800d6098c720802d60a7e720906d60bb27206730900d60c7e8c720b0206d60d7e8cb2db6308a7730a000206d60e7e8cb27206730b000206d60f9a720a730cedededededed938cb27206730d0001730e93c27207d07201938c7208017203927209730f927ec1720706997ec1a7069d9c720a7e7310067e73110695938c720b017203909c9c720c720d7e7204069c720f9a9c720e7e7205069c720d7e720406909c9c720e720d7e7204069c720f9a9c720c7e7205069c720d7e72040690b0ada5d90110639593c272107312c1721073137314d90110599a8c7210018c72100273157316",
           |    "address": "ynjTGtEt9xdN4LPAreFLJVwwdzZgP1tG5jqMBtvhF1Rr7bsMjBioWPLamCZBkytNPRuKCvzFq2Ghr76X2wVvzwPEiGUDEyykJDTbrChB8iNS8xARPwijRGZGymCG7E3xMNn7tCX2yZU3qT5a4ngG8RCJrvVjTef2kuU4jKARdXY8xYEJUDLBsUnssoT54ma2N3xzQRi4cAhA5Ap5AbM5YxV63GaBewRuEshjrTJH4dLDx6ApyVNmpVRwaZUuiLHFvdUtqcFsyakNyACaWTiJfPCWygFsBznUnMmfKB84eRjdmdL1jPB8TF4Heus2Hh82KVThdfWxAe1P5TVkovNUgLw7t4DyuTh6Mr9sMxzTPiidDriv1mKhrDGGru6w7uFhJ5fKZFQHskZ9ei2ybz6D9Vmz9tuUtcEWUGn5Jh8hjbYVHe9ckCCT2Sh56bV5DEx4hZ7Egnprt6ADdMU1sLoN4N5youbByQsZBmDxrbRmGEyMQaubBFJfEKGGk8EUjENvXgCiZ14iXhiyWQaoLFP1QzNrAJHeCtYNLq1XTAH1j2ciMw8CChJELXeAAd9nvUheizsg6kUqzsraCscg6FrEntCiC9ZtPkvcD8KEmtzj5bUDh6WNzMkGUuMtWVr3qto1Eu94DQDkzSP6dCZvRHRjribkHeYTQvhyjREEUzpS2a5zsAY1UxBsRJt45pcHFwanfYTg2kAJnhTCm9rE13kgyHcgZGnKpKwCJxEoYLXUiMRZ6icHW1eKCqjnAkT6tQik9fBWXZwkoKLmFVbbvfcT51a",
           |    "assets": [
           |        {
           |            "tokenId": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
           |            "index": 0,
           |            "amount": 100,
           |            "name": "SigUSD",
           |            "decimals": 2,
           |            "type": "EIP-004"
           |        }
           |    ],
           |    "additionalRegisters": {},
           |    "spentTransactionId": "b2799d2dfa796a4e99f92ff48f85cdb80cb513333ab04ceb217e7f68f97190ad",
           |    "mainChain": true
           |}
           |""".stripMargin
      )
      .toOption
      .get
}
