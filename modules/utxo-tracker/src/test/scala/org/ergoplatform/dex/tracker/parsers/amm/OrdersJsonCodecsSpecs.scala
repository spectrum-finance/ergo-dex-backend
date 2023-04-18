package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.{Clock, SyncIO}
import io.circe.syntax._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.{AMMType, ParserVersion}
import org.ergoplatform.dex.tracker.parsers.amm.v2.{N2TOrdersV2Parser, T2TOrdersV2Parser}
import org.ergoplatform.dex.tracker.parsers.amm.v3.{N2TOrdersV3Parser, T2TOrdersV3Parser}
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.ergo.domain.Output
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class OrdersJsonCodecsSpecs extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with CatsPlatform {

  implicit val clock: Clock[SyncIO] = Clock.create

  implicit val e: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  def parser(tid: TokenId): CFMMOrdersParser[AMMType.T2T_CFMM, ParserVersion.V3, SyncIO] =
    T2TOrdersV3Parser.make[SyncIO](tid)

  def parserN2TOrdersV3Parser: CFMMOrdersParser[AMMType.N2T_CFMM, ParserVersion.V3, SyncIO] =
    N2TOrdersV3Parser.make[SyncIO](TokenId.fromStringUnsafe(""))

  def parserN2TOrdersV2Parser: CFMMOrdersParser[AMMType.N2T_CFMM, ParserVersion.V2, SyncIO] =
    N2TOrdersV2Parser.make[SyncIO]

  def parserT2TOrdersV2Parser: CFMMOrdersParser[AMMType.T2T_CFMM, ParserVersion.V2, SyncIO] =
    T2TOrdersV2Parser.make[SyncIO]

  property("Encode decode correctly") {
    val depositYIsSpectrum = parser(TokenId.fromStringUnsafe("003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0")).deposit(depositV3).unsafeRunSync().get

    val swap = parser(TokenId.fromStringUnsafe("003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0")).swap(swapV3).unsafeRunSync().get

    val swapBuyErgFeeV2 = parserN2TOrdersV2Parser.swap(swapBuyErgFee).unsafeRunSync().get

    val swapSellTokenV3 = parserN2TOrdersV3Parser.swap(swapSellTokenFee).unsafeRunSync().get

    val resList: List[CFMMOrder.AnyOrder] =
      List(depositYIsSpectrum, swap, swapSellTokenV3, swapBuyErgFeeV2)

    val json = resList.map(_.asJson)

    val decoded = json.map(_.as[CFMMOrder.AnyOrder])

    resList.zip(decoded).foreach { case (order, result) =>
      order shouldEqual result.toOption.get
    }

  }

  def swapSellTokenFee =
    io.circe.parser
      .decode[Output](
        s"""
           |{
           |    "boxId": "a3673cf919314fc4ac42929a076ba8c2cbda0193878b2fc739a7fe557e52e70c",
           |    "transactionId": "14556ddac7ba7e8a4fd13b691c2eaf3836a265c9d11a1ba20c5496f50b43a510",
           |    "blockId": "9a9af9e7041985dc09309829bf955a5fe6b63bfaaf0acc9b11474d12d174a500",
           |    "value": 10310000,
           |    "index": 0,
           |    "globalIndex": 26613367,
           |    "creationHeight": 941734,
           |    "settlementHeight": 941736,
           |    "ergoTree": "19800521040005c80105320580dac40904c60f08cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec04040406040204000101052404000e201d5afc59838920bb5ef2a8f9d63825a55b1d48e269d7cecee335d637c3ff5f3f0e240008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec0e20003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d005280101010105f015060100040404020e20003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d00101040406010104d00f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730405000500058092f4010100d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c7e997209730b067e7202067e7203067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7313069d9c720a7e7203067e72020695ed91720b731492b172077315d801d60cb27207731600ed938c720c017317927e8c720c0206720b7318909c7e8cb2720573190002067e7204069c9a720a731a9a9c7ec17201067e731b067e72040690b0ada5d9010b639593c2720b731cc1720b731d731ed9010b599a8c720b018c720b02731f7320",
           |    "address": "ZnQPhHYW6FNzr1CpzGutiwrC1RCaoZxrhwTt7NjasvE5rMC1YQptuMqdApAnt8rNqqGV38j9E6W7dLRrBUqzzGZFh5KUhAbowhJjoiaH5EovkEhNWVfGQnw5LL9cdsYBHBk7aopYFKgcNTYqcRCPi1jEs5PAMrZzLqxFQf9zFJUmH4kmRzhEnfp4TzC1csg247kMqEp2piYFXDLQcDxPpw1KTLykzERp3rH8RWgL9STCM7FCXUTwBL2yFo9tfbM7ZfH9ngNgJ6yaEQFS52cyEDTiWQpbSqCD9vy9Yqtth93xY57etuq65EJu4vcC7TfFrjjnxgAzGR41xjhZaXSkGNcSDE2Zn9LZm9kpE8whDhsgKmA1LonFRfvvusa4KJbFAHLK779MeATJEBEHSYFAVapwLKEPvaYJZ9TDhoaCfjG8Afj4Tp1RpWu3S7C3tJRsjsR9rrejqsDSH1XaxcjG4VJeAm9BDX4SiNnNm7pUTS7LQP6YTET49k2XHrzv8y5yavPJqFSEVD4X7K8QHiRWex7Axiqc9fduQLQ4qmmGDzErdNUnasEUoyNGzPFuTjq3uvroXkuBoGC1vzMmNfQhuV89gNFweDaEULH124Z9fzkJ39KxCXqzeAqhE8y84YFzb6hM1t3FqYM5cZdC4Vos5bWrkLu8qVFoYuVWT1EC4dLdMbRP1pZDSDQvv1a54xXvhWxiTPFhBTSnx3VphpiAxeoqsQRXc14L7n79WUc935fe4mHktk6zXasKewQmaaT4MnunDruqnDBVBoBBGWhxJc3RLSkJTv73qBHyYbBMgz6zzxtfbEzpi7RvM8asREfaTwBWZ4nqxoXVYCQgP7cpaoAw7ZCWKiZeHghujNpZs9ZAnUUtNrAy",
           |    "assets": [
           |        {
           |            "tokenId": "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
           |            "index": 0,
           |            "amount": 18,
           |            "name": "SigRSV",
           |            "decimals": 0,
           |            "type": "EIP-004"
           |        }
           |    ],
           |    "additionalRegisters": {},
           |    "spentTransactionId": "e05ca4f19a066e24ca53a17d9511e7395c9ca16be37296fb41d778bc26310c34",
           |    "mainChain": true
           |}
           |""".stripMargin
      )
      .toOption
      .get

  def swapBuyErgFee =
    io.circe.parser
      .decode[Output](
        s"""
           |{
           |    "boxId": "1df6f07bf4334812ee8376c035e62795567fd9904195a972c25425d15321aab7",
           |    "transactionId": "e611dec1c41f5efe281092bd72f63cfe0c5c0cd7639fe78d832807dca923c559",
           |    "blockId": "0b36bf5e78c677446726af56a65a6bf3533dee2ff6b795de407cb4f71312e0e8",
           |    "value": 51650000,
           |    "index": 0,
           |    "globalIndex": 23349051,
           |    "creationHeight": 508928,
           |    "settlementHeight": 868982,
           |    "ergoTree": "101604000100040404060402050a05f5d9c409040004000e204ad32364b11b0fc1cc66e6528b5ad8cbc31f2f63793357316bf15c8ef283ad4c0eb40319b1031208cd033d6ab05cfb8a65938e116cb863cad577e560bb8e110113bf395fbe98649dbb59040004040406040204000404040005feffffffffffffffff01040204000e209916d75132593c8b07fe18bd8d583bda1652eed7565cf41a4738ddd90fc992ec0580b6dc050e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730405000500058092f4010100d802d6017300d602b2a4730100eb027201d195ed93b1a4730293b1db630872027303d806d603db63087202d604b2a5730400d605b2db63087204730500d606b27203730600d6077e8cb2db6308a77307000206d6087e9973088cb272037309000206ededededed938cb27203730a0001730b93c27204d07201938c7205018c720601927e9a99c17204c1a7730c069d9c72077ec17202067208927e8c720502069d9c72077e8c72060206720890b0ada5d90109639593c27209730dc17209730e730fd90109599a8c7209018c72090273107311050604c80f060101040404d00f04c80f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304050005000580dac4090100d801d601b2a4730000d1ec730195ed93b1a4730293b1db630872017303d804d602db63087201d603b2a5730400d6049d9c7e99c17203c1a7067e7305067e730606d6058cb2db6308a773070002edededed938cb2720273080001730993c27203730a9272047e730b06909c9c7ec17201067e7205067e730c069c9a7204730d9a9c7e8cb27202730e0002067e730f067e9c72057e7310050690b0ada5d90106639593c272067311c1720673127313d90106599a8c7206018c72060273147315",
           |    "address": "o9iYvHqCdt9nYCKYgs2md7cQUgfVHYtYz5rt5CdWcJ322bc4LEGCwfUbb3yEANBM9yGK7qriwCkuFEeseLJABcncPaXkeZXKfQ9PfjnK8nCFKWgqrVd3ob8Cc1bhKK2eUzC5Rzpd9xxABcDuuBrcxNNCocT9qBwHh1FtuDr3iFQowbPuAG5xyNowfzrkTCm2nJ3HvhdYgDQTsJnkfSrAoUuAvvYKH4AqfnURYPpXxWK2ENzxoVtQB8GVCU6K4xGLftzGBMWisuucuMdhFuHA6rMgzjyj1CdNwYrN2sLjFoNB78QptXBZTQowncLGQd3MieYd5LN1VdFDvfAHfShTeufTntNAQJZsczaExKVcnraVqqbqgegar7yxqYajFzaBgumcGKHBqArVscDodUDRxJWkBnQP6zqX9Z9zztX4THysfiKBMdWTKCRTwAAVAmupZnaUVfELmcQnMtfk52fDWht66sHodRsbuaRVLiGW6g8dbbwagxqBYy9L98uKMqHaA1ZyYpFuYKza7HvUGZrFWQjVmsacPgk34PjJhWhDYAJjE4pGyZ47dFmvGFrS781yXiryaWceDXKQoSSpPkXuyBFN1Zc7REtjHD55SZVKL7H1jpP8Ex7sFYhjbc879J9BQz1EZiaV9gNHbxjyf3mhUaxZdBTs2syEW3V2PUptviUp6SbxWkDnusk9tksTUbYbWHEHtiJc9Bqyu1ijFvh4ooicFuHdyQJi3njpqSe6rBznZiGF2ePyAdB52NMRAZ7MdoTuVvWrTDGvF4DATnjn4vV7v1ngyYHyNarXo48NytN9pET1sbdnfhvtfc2hKZzWLPdGyyvSAYykcDXG6C3CR3fEL9wYfHXbQNKgtTGiehs1ByXhsQHJg6PJsTrV6hAHquiruuC9y86PcMsKydrveRTbN5WZK9Vi22tEiN5GeF2Y4j3zx8ZRAdJ6gnqXUraEX8Ds1Zdp4QSptVAbhVmx6TmD9rApSwk26f5s4TJj2jZvDRKZd4p1u36cq5fVAsqZrhwVv8bRkuu8QKEqt2ADyDLotmerovhE544nJ8U6nMMh3WxEFvCbh9ANgYVyeExLWGPZ9i9MSmpW2fmniqpaXHJevrwFd3B94A2PEQF",
           |    "assets": [
           |        {
           |            "tokenId": "30974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e",
           |            "index": 0,
           |            "amount": 10000000,
           |            "name": "WT_ADA",
           |            "decimals": 8,
           |            "type": "EIP-004"
           |        }
           |    ],
           |    "additionalRegisters": {},
           |    "spentTransactionId": "8dde27e825c347334d30d6be7078df736c450e015cc243156878056f709d62e6",
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
}
