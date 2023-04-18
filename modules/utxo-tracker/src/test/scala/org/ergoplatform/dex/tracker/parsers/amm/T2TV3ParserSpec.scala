package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.{Clock, SyncIO}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.{AMMType, ParserVersion}
import org.ergoplatform.dex.tracker.parsers.amm.v3.T2TOrdersV3Parser
import org.ergoplatform.ergo._
import org.ergoplatform.ergo.domain.Output
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class T2TV3ParserSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with CatsPlatform {

  implicit val clock: Clock[SyncIO] = Clock.create

  implicit val e: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  def parser: CFMMOrdersParser[AMMType.T2T_CFMM, ParserVersion.V3, SyncIO] =
    T2TOrdersV3Parser.make[SyncIO](
      TokenId.fromStringUnsafe("003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0")
    )

  property("Parse deposit correct") {
    val deposit = parser.deposit(depositV3).unsafeRunSync().get.asInstanceOf[DepositTokenFee]

    val expected = CFMMOrder.DepositTokenFee(
      PoolId.fromStringUnsafe("5703a5b955c4902f165215f5ce1426816b4c6dca5300cfae35c8a35649287154"),
      2000000,
      deposit.timestamp,
      DepositParams(
        AssetAmount(
          TokenId.fromStringUnsafe("472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e8"),
          1000000
        ),
        AssetAmount(TokenId.fromStringUnsafe("d71693c49a84fbbecd4908c94813b46514b18b67a99952dc1e6e4791556de413"), 40),
        dexFee = 15,
        redeemer =
          SErgoTree.unsafeFromString("0008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec")
      ),
      depositV3
    )

    deposit shouldEqual expected
  }

  property("Parse redeem correct") {
    val redeem = parser.redeem(redeemV3).unsafeRunSync().get.asInstanceOf[RedeemTokenFee]

    redeem.params shouldEqual RedeemParams(
      AssetAmount(TokenId.fromStringUnsafe("7338937c785551c3db0d6db218d8b7df86960d9a5720c2c97c968c21cf5c54f2"), 716),
      15,
      SErgoTree.unsafeFromString("0008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec")
    )
    redeem.poolId shouldEqual PoolId.fromStringUnsafe(
      "5703a5b955c4902f165215f5ce1426816b4c6dca5300cfae35c8a35649287154"
    )
    redeem.maxMinerFee shouldEqual 2000000
  }

  property("Parse swap correct") {
    val swap = parser.swap(swapV3).unsafeRunSync().get.asInstanceOf[SwapTokenFee]

    swap.params shouldEqual SwapParams(
      AssetAmount(
        TokenId.fromStringUnsafe("03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04"),
        2
      ),
      AssetAmount(
        TokenId.fromStringUnsafe("003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0"),
        26
      ),
      5769230769230769L,
      10000000000000000L,
      SErgoTree.unsafeFromString("0008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec")
    )
    swap.poolId shouldEqual PoolId.fromStringUnsafe("080e453271ff4d2f85f97569b09755e67537bf2a1bc1cd09411b459cf901b902")
    swap.maxMinerFee shouldEqual 2000000
  }

  def swapV3 =
    io.circe.parser
      .decode[Output](
        s"""
           |{
           |    "boxId": "436b80973fe567fba7e6179272835071ac92bfb20c54011d114b8e076506e251",
           |    "transactionId": "41ad07b62938750772f1331be0f55d53ab51fe2a7b9480db6fa3376da52a43a3",
           |    "blockId": "6fc118469010e5c1bc2a88c5307f0f3d8a54de7f37e11f070dd161364f7c0efa",
           |    "value": 310000,
           |    "index": 0,
           |    "globalIndex": 26637735,
           |    "creationHeight": 942255,
           |    "settlementHeight": 942257,
           |    "ergoTree": "19cc052304000e20003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d005808088fccdbcc323050404c60f04d00f08cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec040404080402040001010524059eb1839297f7830f0404040606010104000e20080e453271ff4d2f85f97569b09755e67537bf2a1bc1cd09411b459cf901b9020e240008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec05340100010105f015059c01060100040404020e20003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d001010e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730405000500058092f4010100d805d601b2a4730000d6027301d6037302d6049c73037e730405d6057305eb027306d195ed92b1a4730793b1db630872017308d80ad606db63087201d607b2a5730900d608db63087207d609b27208730a00d60a8c720902d60b95730b9d9c7e99720a730c067e7203067e730d067e720a06d60cb27206730e00d60d7e8c720c0206d60e7e8cb27206730f000206d60f9a720b7310ededededededed938cb2720673110001731293c272077313938c720901720292720a731492c17207c1a79573157316d801d610997e7317069d9c720b7e7318067e72030695ed917210731992b17208731ad801d611b27208731b00ed938c721101731c927e8c721102067210731d95938c720c017202909c720d7e7204069c720f9a9c720e7e7205067e720406909c720e7e7204069c720f9a9c720d7e7205067e72040690b0ada5d90110639593c27210731ec17210731f7320d90110599a8c7210018c72100273217322",
           |    "address": "EythtiXp53PKzShomQcugMHAUeCJrX1LGoSowRAbwH7iVzGwVG4p7ougpD9P4QpshLVhdoyjQ4FaHaPHn3p3fFGfLh1LnwzswFYoHZD7RUvv9A9MSTbmNuipZy6e6S9P1gptkyevtAYPgx6BddwyqePwjWhjT9ew9ZbmPN1fsof8ZJHVWVEv5GUJsBtyXrZhUs3abhJHuFvUpvExWEgSRBSEqn3E92xMMTPs81gDjAcDEdmWAJypNnGSp7q6PTVkDVq3YQPDbQn9DknKTmUs9frRCtUwNjWRZQ2WYBE8qxEbttM6cKujnv2BJjgDrxwRGsXnr6WG6dp91ZXgLU3aE5zMSsgUUHw5NMXQQBHrRs1pdqY5jBh3LjcoBJ1QLf6udA4ZjnmeRpy5vtJA2ZTFLgbKPhBmGRhDNTz7LTdMACyR2CKTv9Df7AZmNko1FEff7nMBR7R8s39TpVFZP74iYsFtFMhL9FfFneY3Np3JEu3pwpe43yiso4E5enb5M9fNXMctkJ7aW6edsBKZnPyVcwMgUTS5DPm6g9LJhXBt8ouwbyShE6T2TSpBSwo6gX9AvjMMVXYJ4Z317JGJNXE1u8U5DVWHLAB4WGWrUa7gjBqmrHeBu6PoCyPYcHCFHYaBcHUoV4sX64cbSfML8UZnQ4sd3YWGuGS9YjT74WXsgJYnaMY8ummGMaLDax5EqgAEQ1RzhEinC5j7MmoPdqg2kMLHjvYmfykcjJohaP7adzELnK2TxATj39mw9mpdADGMDpq5BFHFoAdeNPg24K8t9ZK2qSvPW4GWZuM5YxaW1qLyCvAuE5s5Zd4zFvQxVQesGvLNXkcSWVwCp34sqf4XbjsVPqwmMa77KXz5rnnVjwB4u8JwBe3LLqua5pTpnPkSwTtuJyGBDFHtwwCv7cRaHRkZZZ45Fo1ZhWuuep9cgpijyTEWSW6Qqa7NPejgQnwmd9ts1XxrsFHS9KurxWvF1s2iPAAJ",
           |    "assets": [
           |        {
           |            "tokenId": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
           |            "index": 0,
           |            "amount": 2,
           |            "name": "SigUSD",
           |            "decimals": 2,
           |            "type": "EIP-004"
           |        },
           |        {
           |            "tokenId": "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
           |            "index": 1,
           |            "amount": 18,
           |            "name": "SigRSV",
           |            "decimals": 0,
           |            "type": "EIP-004"
           |        }
           |    ],
           |    "additionalRegisters": {},
           |    "spentTransactionId": "8fe0518b8c0ed7069e8f0040ac8d97da869fff41b12cd97a399584957065fcf8",
           |    "mainChain": true
           |}
           |""".stripMargin
      )
      .toOption
      .get

  def redeemV3 =
    io.circe.parser
      .decode[Output](
        s"""
           |{
           |    "boxId": "e8224097dbf46a6f9e90d059917bba2b5ea6714ac895d47a35d15346988a7de7",
           |    "transactionId": "fc8266f5c7893f75f28575818bdc0803f195532609b9f404e629ae35d6348e51",
           |    "blockId": "0fe442d7620bbd1d64ffb77e262de74647617e45cc7bb05db15213c195dbd982",
           |    "value": 310000,
           |    "index": 0,
           |    "globalIndex": 26671828,
           |    "creationHeight": 943019,
           |    "settlementHeight": 943021,
           |    "ergoTree": "19eb0314040008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec0404040804020400040404020406040005feffffffffffffffff01040204000e205703a5b955c4902f165215f5ce1426816b4c6dca5300cfae35c8a356492871540e240008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730405000500058092f4010100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d809d602db63087201d603b2a5730400d604db63087203d605b27204730500d606b27202730600d607b27204730700d608b27202730800d6097e8cb2db6308a77309000206d60a7e99730a8cb27202730b000206edededededed938cb27202730c0001730d93c27203730e938c7205018c720601938c7207018c720801927e8c720502069d9c72097e8c72060206720a927e8c720702069d9c72097e8c72080206720a90b0ada5d9010b639593c2720b730fc1720b73107311d9010b599a8c720b018c720b0273127313",
           |    "address": "5cZpjmfCNTqef1W69zmP1CqswQeB1eBQNEKdvvbf9Ei8Y7kSCRmUvSJmtbuRDSSZxSWTt7w5PXXFrcuZEBxHYuyFqcU483BcmVqHdY3ExjvmpuHYuuMF912TNji6SZZQehiXT1L3gXZeLVZ3zvC3KwVw1fZXrpLfGeutmR358KzMcnWVH4T2LPf8fTk6WsP7y4ygjHpzYqbo3oyxAnyFfRkgQNZ9aUtYVrdkVTpecsQ2p4pB5jWsqnv3kJeMsNhDKxYATLqDwquWoRUw9nExRF8HsXqMdC8WRvbwyxgYppnaFqLaSi41k88s7RUrUjEdGicHiSBSoVrxtrB3ozLWFZNoJjPw169xUB1Gpb64EZkKVqLcnMRRdAsWG2F8GgU3Mi3euv2mBjpEBo7cNu9cTkaniarSksnBi7NTB1MuerTZkuKD6vVEr3HhTkhU97BqP9VUHiUqwmRJuVGyd9JkD3ZsNfR734mohFhgkt1vwKEh57goQvFx2tpAiZZGYbMQoZ6D4hQ5cLieGFMRx633QG2tX7ErswQ74mgD8hTJbu3L64vGnh7YewVaBZMgFX55ABYscEnbsqFgEQCRteHogcd1hbdjq53YBS8Q9yZvnPgofNeKKuQyt2XmpC6H1BLhZWtxxpT1k6dfqsBgiK5ZSzS7Z45UJGgziQTiez2Aq",
           |    "assets": [
           |        {
           |            "tokenId": "7338937c785551c3db0d6db218d8b7df86960d9a5720c2c97c968c21cf5c54f2",
           |            "index": 0,
           |            "amount": 716,
           |            "name": "NETA_ergopad_LP",
           |            "decimals": 0,
           |            "type": "EIP-004"
           |        },
           |        {
           |            "tokenId": "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
           |            "index": 1,
           |            "amount": 15,
           |            "name": "SigRSV",
           |            "decimals": 0,
           |            "type": "EIP-004"
           |        }
           |    ],
           |    "additionalRegisters": {},
           |    "spentTransactionId": "186d63efe0afa5e89181c1da28681bc4c8d8c68d5c03bc4cf75cf9e82401bdd4",
           |    "mainChain": true
           |}
           |""".stripMargin
      )
      .toOption
      .get

  def depositV3 =
    io.circe.parser
      .decode[Output](
        s"""
           |{
           |    "boxId": "9efb46584856e7295cee7aacd41da2c3c7dc4b9aafb5ea5c5b4a983303a835c9",
           |    "transactionId": "dc3aa8a43254ee984cf61b9a30026b3ed7abf9ea1866f8d170c192b3ae21b118",
           |    "blockId": "47c3859bc1e2646bd73c82258e9ad9919d090a1d4a9b27e21ef22978d40d0eb2",
           |    "value": 310000,
           |    "index": 0,
           |    "globalIndex": 26669245,
           |    "creationHeight": 942973,
           |    "settlementHeight": 942975,
           |    "ergoTree": "19e2041a040008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec040404080402040205feffffffffffffffff0104040580897a04060550040004000e205703a5b955c4902f165215f5ce1426816b4c6dca5300cfae35c8a356492871540e240008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec0404040204040402010101000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730405000500058092f4010100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d80cd602db63087201d603b2a5730400d604b27202730500d6057e9973068c72040206d606b27202730700d6077e8c72060206d6089d9c7e73080672057207d609b27202730900d60a7e8c72090206d60b9d9c7e730a067205720ad60cdb63087203d60db2720c730b00edededededed938cb27202730c0001730d93c27203730e92c17203c1a795ed8f7208720b93b1720c730fd801d60eb2720c731000ed938c720e018c720901927e8c720e02069d9c99720b7208720a720595ed917208720b93b1720c7311d801d60eb2720c731200ed938c720e018c720601927e8c720e02069d9c997208720b7207720595937208720b73137314938c720d018c720401927e8c720d0206a17208720b90b0ada5d9010e639593c2720e7315c1720e73167317d9010e599a8c720e018c720e0273187319",
           |    "address": "e5xiryCNuYas2qLeihB1qHYZ2mM21u1wMEKuFD8xJ8Ldu7eFoUH3Hroh39dKRxjZpzu2C5GxdtHinr7miafrQApdwsV81633E3Lh3tZq2tA6nFMsVHkr2Jtbj3PM8MczxuHvhbDNFFU6sSFp6X35pdjkJUknirzeVnG2y6P7UuJ93KT6a6pARrKc21eCgUAYcpc5hFG6Y4zCRDUCRsCE7yBvvkuSpfDXnfULwmPyZTE8ey7R81yuNBkdxeYJ7gwGeE3nFcbCdBEZ1EJTXCiXYNYjYrM9P6uhhQXPL5ueJdPwtBoA8aM6ELJhopnZeNPQ6NiUxStPqAhBFcCwSYYdQp22HPS8CV5RkiTrYvW6egvG7Y6ouu7kwTcSaWZXat9KStcs4c4PHm68BvazFy1Jsu9XH5mGnryy2Riivr9wcHQxa8r4YD2EjLshZGpZomGhZgkymwvJL5FbikLSevH1ok7cMS2BYBKPVbZFbhSAwDpfxQimff32BrnBRmki8prdSxojjApx3YoADmCBKLcpEaqW1eZGcEHHWrGN2d8e6zUfkHsfkcyobUxMUx1DsM5y6tM9PHgiUHYQ46JiMxb1LK91cuPjfMu7sa9agoLxF6tNKNL4uHWzZB4J9GsygxgpaiY5k9grqBMCfQJ9AdKdwfKrZiAE4PPXHX1P6ri93LjfVpwFBkYxEDz9vZPeieDoaqWQquhXvMGmDfYuyQ1u3xw7ya1aS3xSsRt4QHR4t3aurL7dFRMzAV4VUD3nJza58e7U4xFECQJEJuu6Wc46mhQG2oDXmS3tEvFH5jZc83BzXhh2VVEBEWLB7mFtYKN33KGFDs7Kk9Y",
           |    "assets": [
           |        {
           |            "tokenId": "472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e8",
           |            "index": 0,
           |            "amount": 1000000,
           |            "name": "NETA",
           |            "decimals": 6,
           |            "type": "EIP-004"
           |        },
           |        {
           |            "tokenId": "d71693c49a84fbbecd4908c94813b46514b18b67a99952dc1e6e4791556de413",
           |            "index": 1,
           |            "amount": 40,
           |            "name": "ergopad",
           |            "decimals": 2,
           |            "type": "EIP-004"
           |        },
           |        {
           |            "tokenId": "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
           |            "index": 2,
           |            "amount": 15,
           |            "name": "SigRSV",
           |            "decimals": 0,
           |            "type": "EIP-004"
           |        }
           |    ],
           |    "additionalRegisters": {},
           |    "spentTransactionId": "867843e396896a54c4cc3754fda591dd3d0a1880d2100876550f28d7f44581c3",
           |    "mainChain": true
           |}
           |""".stripMargin
      )
      .toOption
      .get
}
