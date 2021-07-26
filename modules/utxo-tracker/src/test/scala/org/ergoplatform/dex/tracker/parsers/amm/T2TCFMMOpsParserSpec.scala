package org.ergoplatform.dex.tracker.parsers.amm

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.{PoolId, Swap, SwapParams}
import org.ergoplatform.ergo.models.{BoxAsset, Output}
import org.ergoplatform.ergo._
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class T2TCFMMOpsParserSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks {

  property("Swap order parsing") {
    val res = parser.swap(boxSample)
    println(res)
    res shouldBe Some(
      Swap(
        PoolId.fromStringUnsafe("f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc781"),
        SwapParams(
          AssetAmount(
            TokenId.fromStringUnsafe("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b"),
            1000000000,
            Some("WT_ERG")
          ),
          AssetAmount(
            TokenId.fromStringUnsafe("30974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e"),
            1991404171,
            None
          ),
          dexFeePerTokenNum = 5021582331515564L,
          dexFeePerTokenDenom = 1000000000000000000L,
          p2pk = Address.fromStringUnsafe("9g1N1xqhrNG1b2TkmFcQGTFZ47EquUYUZAiWWCBEbZaBcsMhXJU")
        ),
        Output(
          BoxId("25bfa760264edb7af467173c7399c65df65bcdd9a3612fd1755f1140b0e6eafd"),
          TxId("4655d2caeb7cb5e269cb05f89341442269a0bfbf27b06bccba50ee67e1e0c1c5"),
          12050000L,
          0,
          5740337,
          506880,
          540435,
          SErgoTree.unsafeFromString(
            "19ca031008cd02c3f56e66191a903758f53a4b90d07cef80f93e7a4f17d106098ad0caf189722a040004040e2030974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e0406040004d00f04c80f04000e20f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc78104000601010596aa93eb0e05d8caa4f1c7c6eb11058080a0f6f4acdbe01b0100d80fd6017300d602db6308b2a4730100d603b27202730200d6048c720301d6057303d606b27202730400d6078c720601d608b2db6308a7730500d6098c720801d60a7e8c72060206d60b7306d60c7e8c72080206d60d7307d60e9c720c7e720d06d60f7e8c72030206eb027201d1ededed938cb27202730800017309ec93720472059372077205ec93720472099372077209aea5d9011063d804d612b2db63087210730a00d6138c721202d6147e721306d6159a7214730bededededed93c27210d07201938c7212017205927213730c927ec1721006997ec1a7069d9c72147e730d067e730e06959372047205909c9c720f720c7e720d069c72159a9c720a7e720b06720e909c9c720a720c7e720d069c72159a9c720f7e720b06720ee5dc2407c67210040e01d901160e937216c5a7730f"
          ),
          Address.fromStringUnsafe(
            "4X2w8UDMo2aKgYhXEeRCpKf2H6ACsPGZ23kkuFkUt8eiMk7so9y9stkmVGrh3hg6pDqruncEMtoibxyBJpjKFpwbtoMhUWmGkP4RUCB6nCFgSo47FsABiVSawdA1bBDRxthJArShCz5CKjMQTmrX7iwqRXD5wEmdjEirhaLC4u9opC6BC1pbbcgfq2WJZxYW4eK4MSsjhBTXT2VoXbs36BABW6hvAnw9etSTTH9vXLUJnHuB3x84DwqDRtDvGEaD6i6YtHJLZ6ua6Kxyj8pLWqxkX5tLXf3p8ByQ9tiuybYTV42Zrgrq2Z2j2m1kqcMgFT6kdt8WzN9Y69XemFJvHm3brS2AZPCVsrjdc6RynU2wB7f9vvTxCXkuiGtWyBuiRtE1q1fXMEwXredQ55MgFpaWB1VQc9RK1dPWSUpsFnSseXRZiAyasfCDQVX1A5AzkW9Xk2v44ZsiEMkfF1usE2iJyAAFfSXWu6w4Qahg116EUmdK4bszbZfnbi7zYuscuBbiKy5UN1JtbASbFteZSPbbWtgYLt9FmSs16yccn1D4gYYNHN7Jegn1zyuWVG1pQQyDERzrrNDdjpznuvKswDJbkTtn5M4aWdTTrPK8c3uK4zD7hZ5rz29W9PFd"
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
  def parser                         = new T2TCFMMOpsParser

  def boxSample =
    io.circe.parser
      .decode[Output](
        """
           |{
           |    "boxId": "25bfa760264edb7af467173c7399c65df65bcdd9a3612fd1755f1140b0e6eafd",
           |    "transactionId": "4655d2caeb7cb5e269cb05f89341442269a0bfbf27b06bccba50ee67e1e0c1c5",
           |    "blockId": "20930b03ca750874c3d4c5f8273a90aa7dde413266c57150c12ba13c453e0ca8",
           |    "value": 12050000,
           |    "index": 0,
           |    "globalIndex": 5740337,
           |    "creationHeight": 506880,
           |    "settlementHeight": 540435,
           |    "ergoTree": "19ca031008cd02c3f56e66191a903758f53a4b90d07cef80f93e7a4f17d106098ad0caf189722a040004040e2030974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e0406040004d00f04c80f04000e20f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc78104000601010596aa93eb0e05d8caa4f1c7c6eb11058080a0f6f4acdbe01b0100d80fd6017300d602db6308b2a4730100d603b27202730200d6048c720301d6057303d606b27202730400d6078c720601d608b2db6308a7730500d6098c720801d60a7e8c72060206d60b7306d60c7e8c72080206d60d7307d60e9c720c7e720d06d60f7e8c72030206eb027201d1ededed938cb27202730800017309ec93720472059372077205ec93720472099372077209aea5d9011063d804d612b2db63087210730a00d6138c721202d6147e721306d6159a7214730bededededed93c27210d07201938c7212017205927213730c927ec1721006997ec1a7069d9c72147e730d067e730e06959372047205909c9c720f720c7e720d069c72159a9c720a7e720b06720e909c9c720a720c7e720d069c72159a9c720f7e720b06720ee5dc2407c67210040e01d901160e937216c5a7730f",
           |    "address": "4X2w8UDMo2aKgYhXEeRCpKf2H6ACsPGZ23kkuFkUt8eiMk7so9y9stkmVGrh3hg6pDqruncEMtoibxyBJpjKFpwbtoMhUWmGkP4RUCB6nCFgSo47FsABiVSawdA1bBDRxthJArShCz5CKjMQTmrX7iwqRXD5wEmdjEirhaLC4u9opC6BC1pbbcgfq2WJZxYW4eK4MSsjhBTXT2VoXbs36BABW6hvAnw9etSTTH9vXLUJnHuB3x84DwqDRtDvGEaD6i6YtHJLZ6ua6Kxyj8pLWqxkX5tLXf3p8ByQ9tiuybYTV42Zrgrq2Z2j2m1kqcMgFT6kdt8WzN9Y69XemFJvHm3brS2AZPCVsrjdc6RynU2wB7f9vvTxCXkuiGtWyBuiRtE1q1fXMEwXredQ55MgFpaWB1VQc9RK1dPWSUpsFnSseXRZiAyasfCDQVX1A5AzkW9Xk2v44ZsiEMkfF1usE2iJyAAFfSXWu6w4Qahg116EUmdK4bszbZfnbi7zYuscuBbiKy5UN1JtbASbFteZSPbbWtgYLt9FmSs16yccn1D4gYYNHN7Jegn1zyuWVG1pQQyDERzrrNDdjpznuvKswDJbkTtn5M4aWdTTrPK8c3uK4zD7hZ5rz29W9PFd",
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
