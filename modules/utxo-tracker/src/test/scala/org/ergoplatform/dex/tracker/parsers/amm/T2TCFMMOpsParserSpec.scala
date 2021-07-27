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
          BoxId("b33ce0094a6cb1fd4a2ee84b13921ed489de44ed11205c6e3591bb15d593d370"),
          TxId("baf8a292d214e42436328c7b37dc81f176e4806ed4aee82cce218de8a8b7c549"),
          12050000L,
          0,
          5743858,
          506880,
          540620,
          SErgoTree.unsafeFromString(
            "198c031108cd02c3f56e66191a903758f53a4b90d07cef80f93e7a4f17d106098ad0caf189722a0400040204000e2030974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e0404040004c80f040604d00f06010104000e20f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc78104040596aa93eb0e05d8caa4f1c7c6eb11058080a0f6f4acdbe01bd80ed6017300d602db6308b2a4730100d603b2a5730200d604b2db63087203730300d6057304d6068c720402d6077e720606d608b27202730500d6097e8c72080206d60a7e8cb2db6308a77306000206d60b7307d60c7e8cb272027308000206d60d7309d60e9a7207730aeb027201d1eded938cb27202730b0001730c93b1a4730dedededed93c27203d07201938c7204017205927206730e927ec1720306997ec1a7069d9c72077e730f067e73100695938c7208017205909c9c7209720a7e720b069c720e9a9c720c7e720d069c720a7e720b06909c9c720c720a7e720b069c720e9a9c72097e720d069c720a7e720b06"
          ),
          Address.fromStringUnsafe(
            "EPNcS461xwdT5drQxJfoHRPqvQLsvuuJHUBa964oSHwx116u4i9xLjLvDp9VrpfdWGdhJV9bZDrEKzc484Gw94Ws78sR7iMr47fApZpGxTKktxPJRs4ZUXisV75tMT95Tyha1d2D7qpFW2cR1yRGwHKhrnKNBaAs1njAWedU9YrXon3SGWcC6PQ1cZPYKJoZ3YCxdzh3RiYPSrLhmUQtt93AP2eA3PYAsZo7x3Xaokw2isoEj7gos9DR1RNb8SspTKnXwfKfAMVKduX38zhe3xLoxE9LFtwGTU4K7TKj1kQWBjzuUXQYc8dV8QwdzwFLFsFQLnYNrpDiYeDN7uNYivqcusYiYd3VFShtT4rFLFX4sEimvnLyStX8BM5MkBvUCuCt8hSnac21HbHYRWB3xdYK6315i7fYCR92bVmfmSrGVCoPLhJFfwAseZeQ1JSHYLM98wwGW3yHX2m3ScPdRej4Z28XLNX2zPv2a8cYs4Z4LkdN7ratdjV3h4vTmTV8ipLp82zNfzMa6z2Bh8KZx5z5V4z6hSRaFsGh7eH"
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
           |            "boxId": "b33ce0094a6cb1fd4a2ee84b13921ed489de44ed11205c6e3591bb15d593d370",
           |            "transactionId": "baf8a292d214e42436328c7b37dc81f176e4806ed4aee82cce218de8a8b7c549",
           |            "blockId": "8eb54c3e177d4cbd2ebbe72ce78bb0110e21b6e50438204e99a7472ed4669ec6",
           |            "value": 12050000,
           |            "index": 0,
           |            "globalIndex": 5743858,
           |            "creationHeight": 506880,
           |            "settlementHeight": 540620,
           |            "ergoTree": "198c031108cd02c3f56e66191a903758f53a4b90d07cef80f93e7a4f17d106098ad0caf189722a0400040204000e2030974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e0404040004c80f040604d00f06010104000e20f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc78104040596aa93eb0e05d8caa4f1c7c6eb11058080a0f6f4acdbe01bd80ed6017300d602db6308b2a4730100d603b2a5730200d604b2db63087203730300d6057304d6068c720402d6077e720606d608b27202730500d6097e8c72080206d60a7e8cb2db6308a77306000206d60b7307d60c7e8cb272027308000206d60d7309d60e9a7207730aeb027201d1eded938cb27202730b0001730c93b1a4730dedededed93c27203d07201938c7204017205927206730e927ec1720306997ec1a7069d9c72077e730f067e73100695938c7208017205909c9c7209720a7e720b069c720e9a9c720c7e720d069c720a7e720b06909c9c720c720a7e720b069c720e9a9c72097e720d069c720a7e720b06",
           |            "address": "EPNcS461xwdT5drQxJfoHRPqvQLsvuuJHUBa964oSHwx116u4i9xLjLvDp9VrpfdWGdhJV9bZDrEKzc484Gw94Ws78sR7iMr47fApZpGxTKktxPJRs4ZUXisV75tMT95Tyha1d2D7qpFW2cR1yRGwHKhrnKNBaAs1njAWedU9YrXon3SGWcC6PQ1cZPYKJoZ3YCxdzh3RiYPSrLhmUQtt93AP2eA3PYAsZo7x3Xaokw2isoEj7gos9DR1RNb8SspTKnXwfKfAMVKduX38zhe3xLoxE9LFtwGTU4K7TKj1kQWBjzuUXQYc8dV8QwdzwFLFsFQLnYNrpDiYeDN7uNYivqcusYiYd3VFShtT4rFLFX4sEimvnLyStX8BM5MkBvUCuCt8hSnac21HbHYRWB3xdYK6315i7fYCR92bVmfmSrGVCoPLhJFfwAseZeQ1JSHYLM98wwGW3yHX2m3ScPdRej4Z28XLNX2zPv2a8cYs4Z4LkdN7ratdjV3h4vTmTV8ipLp82zNfzMa6z2Bh8KZx5z5V4z6hSRaFsGh7eH",
           |            "assets": [
           |                {
           |                    "tokenId": "ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b",
           |                    "index": 0,
           |                    "amount": 1000000000,
           |                    "name": "WT_ERG",
           |                    "decimals": 9,
           |                    "type": "EIP-004"
           |                }
           |            ],
           |            "additionalRegisters": {},
           |            "spentTransactionId": "6ab329579100d0c1d8db508648a22b40ab3504c03c12480bcd0bf211c2a21c36",
           |            "mainChain": true
           |        }
           |""".stripMargin
      )
      .toOption
      .get
}
