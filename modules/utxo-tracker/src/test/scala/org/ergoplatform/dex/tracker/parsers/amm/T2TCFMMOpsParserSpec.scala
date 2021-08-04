package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.IO
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.{PoolId, Swap, SwapParams}
import org.ergoplatform.ergo.models.{BoxAsset, Output}
import org.ergoplatform.ergo._
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class T2TCFMMOpsParserSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with CatsPlatform {

  property("Swap order parsing") {
    val res = parser.swap(boxSample).unsafeRunSync()
    res shouldBe Some(
      Swap(
        PoolId.fromStringUnsafe("f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc781"),
        FixedTs,
        SwapParams(
          AssetAmount(
            TokenId.fromStringUnsafe("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b"),
            1000000000,
            Some("WT_ERG")
          ),
          AssetAmount(
            TokenId.fromStringUnsafe("30974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e"),
            1901019262,
            None
          ),
          dexFeePerTokenNum = 2630167984063278L,
          dexFeePerTokenDenom = 1000000000000000000L,
          p2pk = Address.fromStringUnsafe("9g1N1xqhrNG1b2TkmFcQGTFZ47EquUYUZAiWWCBEbZaBcsMhXJU")
        ),
        Output(
          BoxId("e419674609fe037d98d07e9c7074b3ad25f2c4e69a9bf844c389117a332fa87d"),
          TxId("0203eb80c9c8ebe09bdc466c779eb687e3f6b6f8f0c176f01a61fc10aca6cdbd"),
          16050000,
          0,
          5972866,
          508928,
          547184,
          SErgoTree.unsafeFromString(
            "19a2031308cd02c3f56e66191a903758f53a4b90d07cef80f93e7a4f17d106098ad0caf189722a04000e2030974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e04c80f04d00f040404080402040004040400040606010104000e20f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc78105fc81fa940e05dc8c9ec6f687ac09058080a0f6f4acdbe01b0100d805d6017300d602b2a4730100d6037302d6047303d6057304eb027201d195ed93b1a4730593b1db630872027306d80ad606db63087202d607b2a5730700d608b2db63087207730800d6098c720802d60a7e720906d60bb27206730900d60c7e8c720b0206d60d7e8cb2db6308a7730a000206d60e7e8cb27206730b000206d60f9a720a730cededededed938cb27206730d0001730e93c27207d07201938c7208017203927209730f927ec1720706997ec1a7069d9c720a7e7310067e73110695938c720b017203909c9c720c720d7e7204069c720f9a9c720e7e7205069c720d7e720406909c9c720e720d7e7204069c720f9a9c720c7e7205069c720d7e7204067312"
          ),
          Address.fromStringUnsafe(
            "H3AA3N1iexioN5zbQ5SzNBVF5GAobDaCxpeUyVmw97EoZhy9Vp7px22pEvB7h2nWNoHC1sjnSkJBg3CqAWYCNierqqxvB1snntGLWQnXRFzVGRRHochsKNVaTEDXJW6jCWdamUH4ss25A37dQwRZn4DN7D8JMNu7pr7bYkfpWtM1dFWRjxiUoWYQiXCST8W8VmS4ns7AEb28kAmCjXssCQPsnZg7epE9MEcmrGnpYdKM4beHsvSJEEPT4vWybvNyugf2gtUKyyVydKLcFgDmx6wYnvZ3odpLqDR14mGAHdBFZyYwBSdhQsPHa3CptTgYF5DAbcckYAX7qP6RS3HBCEH4RnkFy6n64boJZLYEh85Exgm1xvXbrK4qXfH3WbFEhwnFQniE3pRyRSRkjVtw9Zz8SL1qXpzCqtrhgxG7uXX8MasuPi5pnvfXrgEL9CKp1JXkbspzMF5GjGQu78QVRpFdmvTR8M9xULsXXicM4HJAi2ANzkEuUGUsCEVrnn9hjWBNZhMi9iGt5KXoqYsPyZUJKes7rE165D3u6iHJChfpmFC4cKCEsBHEr5SxT8TfAoykE"
          ),
          List(
            BoxAsset(
              TokenId.fromStringUnsafe("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b"),
              0,
              1000000000L,
              Some("WT_ERG"),
              Some(9),
              Some(TokenType("EIP-004"))
            )
          ),
          Map()
        )
      )
    )

  }

  implicit val e: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)
  def parser                         = new T2TCFMMOpsParser[IO]

  def boxSample =
    io.circe.parser
      .decode[Output](
        """
           |{
           |    "boxId": "e419674609fe037d98d07e9c7074b3ad25f2c4e69a9bf844c389117a332fa87d",
           |    "transactionId": "0203eb80c9c8ebe09bdc466c779eb687e3f6b6f8f0c176f01a61fc10aca6cdbd",
           |    "blockId": "ba6012b63585a48610ef9355bc0337bfd0c523841a1871a2e841c9b5e388f191",
           |    "value": 16050000,
           |    "index": 0,
           |    "globalIndex": 5972866,
           |    "creationHeight": 508928,
           |    "settlementHeight": 547184,
           |    "ergoTree": "19a2031308cd02c3f56e66191a903758f53a4b90d07cef80f93e7a4f17d106098ad0caf189722a04000e2030974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e04c80f04d00f040404080402040004040400040606010104000e20f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc78105fc81fa940e05dc8c9ec6f687ac09058080a0f6f4acdbe01b0100d805d6017300d602b2a4730100d6037302d6047303d6057304eb027201d195ed93b1a4730593b1db630872027306d80ad606db63087202d607b2a5730700d608b2db63087207730800d6098c720802d60a7e720906d60bb27206730900d60c7e8c720b0206d60d7e8cb2db6308a7730a000206d60e7e8cb27206730b000206d60f9a720a730cededededed938cb27206730d0001730e93c27207d07201938c7208017203927209730f927ec1720706997ec1a7069d9c720a7e7310067e73110695938c720b017203909c9c720c720d7e7204069c720f9a9c720e7e7205069c720d7e720406909c9c720e720d7e7204069c720f9a9c720c7e7205069c720d7e7204067312",
           |    "address": "H3AA3N1iexioN5zbQ5SzNBVF5GAobDaCxpeUyVmw97EoZhy9Vp7px22pEvB7h2nWNoHC1sjnSkJBg3CqAWYCNierqqxvB1snntGLWQnXRFzVGRRHochsKNVaTEDXJW6jCWdamUH4ss25A37dQwRZn4DN7D8JMNu7pr7bYkfpWtM1dFWRjxiUoWYQiXCST8W8VmS4ns7AEb28kAmCjXssCQPsnZg7epE9MEcmrGnpYdKM4beHsvSJEEPT4vWybvNyugf2gtUKyyVydKLcFgDmx6wYnvZ3odpLqDR14mGAHdBFZyYwBSdhQsPHa3CptTgYF5DAbcckYAX7qP6RS3HBCEH4RnkFy6n64boJZLYEh85Exgm1xvXbrK4qXfH3WbFEhwnFQniE3pRyRSRkjVtw9Zz8SL1qXpzCqtrhgxG7uXX8MasuPi5pnvfXrgEL9CKp1JXkbspzMF5GjGQu78QVRpFdmvTR8M9xULsXXicM4HJAi2ANzkEuUGUsCEVrnn9hjWBNZhMi9iGt5KXoqYsPyZUJKes7rE165D3u6iHJChfpmFC4cKCEsBHEr5SxT8TfAoykE",
           |    "assets": [
           |        {
           |            "tokenId": "ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b",
           |            "index": 0,
           |            "amount": 1000000000,
           |            "name": "WT_ERG",
           |            "decimals": 9,
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
