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
          BoxId("f9fed9d99a7abdad53526b49d6b32d756947e27e108b41c2cf07b4f36b86ca3e"),
          TxId("0d659677014e2915bf93eece039f045edc139e2aff70a2a931aa8cd9ce96d24a"),
          12050000L,
          0,
          5717630,
          506880,
          539689,
          SErgoTree.unsafeFromString(
            "19c8031108cd02c3f56e66191a903758f53a4b90d07cef80f93e7a4f17d106098ad0caf189722a040004040e2030974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e0406040004d00f04c80f04c80f04000e20f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc781040005020596aa93eb0e05d8caa4f1c7c6eb11058080a0f6f4acdbe01b0100d810d6017300d602db6308b2a4730100d603b27202730200d6048c720301d6057303d606b27202730400d6078c720601d608b2db6308a7730500d6098c720801d60a7e8c72060206d60b7306d60c8c720802d60d7e9c720c7e73070506d60e7e8c72030206d60f7e720c06d6107308eb027201d1ededed938cb2720273090001730aec93720472059372077205ec93720472099372077209aea5d9011163d803d613b2db63087211730b00d6148c721302d6159a7214730cededededed93c27211d07201938c7213017205927214730d92c1721199c1a79d9c7214730e730f959372047205909c9c720e720f7e7210069c7e7215069a9c720a7e720b06720d909c9c720a720f7e7210069c7e7215069a9c720e7e720b06720de5dc2407c67211040e01d901160e937216c5a77310"
          ),
          Address.fromStringUnsafe(
            "BUSKSyG8NDXtbapbaDr8iEgceSBdUtVhjABeQxQ8mWFGk2LWhBBCE6Xa4nMJhQjLpgcT9DLbCmtsQxYrXzXBbLdXJvoePMYEppFiKtvcaw5vZZ8pT9TZdiTMsy8VCnkWtxMwTWEB5jfb8yc9ts1Fy61UyzNq8AaFYJNp9DP19eHEkRgJp8NUkMzTNfUsXpYzjmCnKw9G3C2BEYBSBzQoZaQXcUJDXV6uMRqZCUN4mvJwajz4zuBGKyBRLgvPMDYnhhxv986rsjLmU37nMuTAY3b8QrZ3jeTqGSRLG1vyjQxspkW1AUSys5WCL7uzEjGPA1qgu6Fp2zse1ukvdWHYp6yQ2orsKC5W21EumP6Jxs1mHhjAMC4BQjg5ogdncJjg8YxjG3BdLQfm3qntHpa1zNkd5bYuA7ia3RRKyeCBvdeGeqtkbBap1bpTrZ7R5SKgGdBfXHRWgJUrgggnULvCHs57kBo2YoHgNPWhJ4FkZJNoLnmJAviK8ppmKuDMFDosoKGS98R6ctdK3S4C5J1G5XfjviEHnnbvgmZZcmnTE26iZXUWUToCLgSxXt5ShGCPx3LJaCPUgmYtWJYicweospe7KM2Z4E6WrQmAF5PhW6F2g2umH3e53Xuvu"
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
     |    "boxId": "f9fed9d99a7abdad53526b49d6b32d756947e27e108b41c2cf07b4f36b86ca3e",
     |    "transactionId": "0d659677014e2915bf93eece039f045edc139e2aff70a2a931aa8cd9ce96d24a",
     |    "blockId": "be586fe6a85baf1149fae389abf40342261a9691940d2a5704c2d651432f1ea8",
     |    "value": 12050000,
     |    "index": 0,
     |    "globalIndex": 5717630,
     |    "creationHeight": 506880,
     |    "settlementHeight": 539689,
     |    "ergoTree": "19c8031108cd02c3f56e66191a903758f53a4b90d07cef80f93e7a4f17d106098ad0caf189722a040004040e2030974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e0406040004d00f04c80f04c80f04000e20f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc781040005020596aa93eb0e05d8caa4f1c7c6eb11058080a0f6f4acdbe01b0100d810d6017300d602db6308b2a4730100d603b27202730200d6048c720301d6057303d606b27202730400d6078c720601d608b2db6308a7730500d6098c720801d60a7e8c72060206d60b7306d60c8c720802d60d7e9c720c7e73070506d60e7e8c72030206d60f7e720c06d6107308eb027201d1ededed938cb2720273090001730aec93720472059372077205ec93720472099372077209aea5d9011163d803d613b2db63087211730b00d6148c721302d6159a7214730cededededed93c27211d07201938c7213017205927214730d92c1721199c1a79d9c7214730e730f959372047205909c9c720e720f7e7210069c7e7215069a9c720a7e720b06720d909c9c720a720f7e7210069c7e7215069a9c720e7e720b06720de5dc2407c67211040e01d901160e937216c5a77310",
     |    "address": "BUSKSyG8NDXtbapbaDr8iEgceSBdUtVhjABeQxQ8mWFGk2LWhBBCE6Xa4nMJhQjLpgcT9DLbCmtsQxYrXzXBbLdXJvoePMYEppFiKtvcaw5vZZ8pT9TZdiTMsy8VCnkWtxMwTWEB5jfb8yc9ts1Fy61UyzNq8AaFYJNp9DP19eHEkRgJp8NUkMzTNfUsXpYzjmCnKw9G3C2BEYBSBzQoZaQXcUJDXV6uMRqZCUN4mvJwajz4zuBGKyBRLgvPMDYnhhxv986rsjLmU37nMuTAY3b8QrZ3jeTqGSRLG1vyjQxspkW1AUSys5WCL7uzEjGPA1qgu6Fp2zse1ukvdWHYp6yQ2orsKC5W21EumP6Jxs1mHhjAMC4BQjg5ogdncJjg8YxjG3BdLQfm3qntHpa1zNkd5bYuA7ia3RRKyeCBvdeGeqtkbBap1bpTrZ7R5SKgGdBfXHRWgJUrgggnULvCHs57kBo2YoHgNPWhJ4FkZJNoLnmJAviK8ppmKuDMFDosoKGS98R6ctdK3S4C5J1G5XfjviEHnnbvgmZZcmnTE26iZXUWUToCLgSxXt5ShGCPx3LJaCPUgmYtWJYicweospe7KM2Z4E6WrQmAF5PhW6F2g2umH3e53Xuvu",
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
