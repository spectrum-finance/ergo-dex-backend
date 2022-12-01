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
import org.ergoplatform.ergo.domain.Output
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class OrdersJsonCodecsSpecs extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with CatsPlatform {

  implicit val clock: Clock[SyncIO] = Clock.create

  implicit val e: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  def parser: CFMMOrdersParser[AMMType.T2T_CFMM, ParserVersion.V3, SyncIO] =
    T2TOrdersV3Parser.make[SyncIO]

  def parserN2TOrdersV3Parser: CFMMOrdersParser[AMMType.N2T_CFMM, ParserVersion.V3, SyncIO] =
    N2TOrdersV3Parser.make[SyncIO]

  def parserN2TOrdersV2Parser: CFMMOrdersParser[AMMType.N2T_CFMM, ParserVersion.V2, SyncIO] =
    N2TOrdersV2Parser.make[SyncIO]

  def parserT2TOrdersV2Parser: CFMMOrdersParser[AMMType.T2T_CFMM, ParserVersion.V2, SyncIO] =
    T2TOrdersV2Parser.make[SyncIO]

  property("Encode decode correctly") {
    val depositYIsSpectrum = parser.deposit(boxSampleDepositYIsSpectrum).unsafeRunSync().get

    val redeem = parser.redeem(boxSampleRedeem).unsafeRunSync().get

    val swap = parser.swap(boxSampleSwapXIsSpectrum).unsafeRunSync().get

    val orderV2 = parserT2TOrdersV2Parser.swap(orderV2Box).unsafeRunSync().get

    val swapBuyErgFeeV2 = parserN2TOrdersV2Parser.swap(swapBuyErgFee).unsafeRunSync().get

    val swapSellTokenV3 = parserN2TOrdersV3Parser.swap(swapSellTokenFee).unsafeRunSync().get

    val resList: List[CFMMOrder.AnyOrder] =
      List(depositYIsSpectrum, redeem, swap, orderV2, swapSellTokenV3, swapBuyErgFeeV2)

    val json = resList.map(_.asJson)

    val decoded = json.map(_.as[CFMMOrder.AnyOrder])

    resList.zip(decoded).foreach { case (order, result) =>
      println(order)
      println(result)
      println("-----")
      order shouldEqual result.toOption.get
    }

  }

  def swapSellTokenFee =
    io.circe.parser
      .decode[Output](
        s"""
           |{
           |    "boxId": "cd29cc599a8a53d28504f2752fc075286359d6b7f1e82582b7e6cd45468a6b29",
           |    "transactionId": "52cdef65d1a93e29774904b5457577e7ec351a6fded878f5ee63733020865495",
           |    "blockId": "274b42d256323aa61a1735f55ca15071a789704b9c66d5daba19151f4199e7e3",
           |    "value": 7260000,
           |    "index": 0,
           |    "globalIndex": 20758110,
           |    "creationHeight": 825489,
           |    "settlementHeight": 825491,
           |    "ergoTree": "19d70422040005f01504d00f010004040406040204000101040804000e2000000000000000000000000000000000000000000000000000000000000000000e2003030303030303030303030303030303030303030303030303030303030303030e20010101010101010101010101010101010101010101010101010101010101010105c00c010101010540055406010004020e2002020202020202020202020202020202020202020202020202020202020202020101040405e01204c80f06010105e01204c80f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005e2f85e0100d803d601b2a4730000d6027301d6037302d1ec730395ed92b1a4730493b1db630872017305d806d604db63087201d605b2a5730600d606db63087205d607b27206730700d6088c720702d6099573089d9c7e720306997e7208067e7202067e7309067e720806edededededed938cb27204730a0001730b93c27205730c938c720701730d9272097e730e0695730f7310d801d60a997e7202069d9c72097e7311067e7312069591720a7313d801d60bb27206731400ed938c720b017315927e8c720b0206720a7316909c9c7e8cb2720473170002067e7318067e7319069c9a7209731a9a9c7ec17201067e7203067e9c731b7e731c050690b0ada5d9010a639593c2720a731dc1720a731e731fd9010a599a8c720a018c720a0273207321",
           |    "address": "ynjTGtEt9xdN4LPAreFLJVwwdzZgP1tG5jqMBtvhF1Rr7bsMjBioWPLamCZBkytNPRuKCvzFq2Ghr76X2wVvzwPEiGUDEyykJDTbrChB8iNS8xARPwijRGZGymCG7E3xMNn7tCX2yZU3qT5a4ngG8RCJrvVjTef2kuU4jKARdXY8xYEJUDLBsUnssoT54ma2N3xzQRi4cAhA5Ap5AbM5YxV63GaBewRuEshjrTJH4dLDx6ApyVNmpVRwaZUuiLHFvdUtqcFsyakNyACaWTiJfPCWygFsBznUnMmfKB84eRjdmdL1jPB8TF4Heus2Hh82KVThdfWxAe1P5TVkovNUgLw7t4DyuTh6Mr9sMxzTPiidDriv1mKhrDGGru6w7uFhJ5fKZFQHskZ9ei2ybz6D9Vmz9tuUtcEWUGn5Jh8hjbYVHe9ckCCT2Sh56bV5DEx4hZ7Egnprt6ADdMU1sLoN4N5youbByQsZBmDxrbRmGEyMQaubBFJfEKGGk8EUjENvXgCiZ14iXhiyWQaoLFP1QzNrAJHeCtYNLq1XTAH1j2ciMw8CChJELXeAAd9nvUheizsg6kUqzsraCscg6FrEntCiC9ZtPkvcD8KEmtzj5bUDh6WNzMkGUuMtWVr3qto1Eu94DQDkzSP6dCZvRHRjribkHeYTQvhyjREEUzpS2a5zsAY1UxBsRJt45pcHFwanfYTg2kAJnhTCm9rE13kgyHcgZGnKpKwCJxEoYLXUiMRZ6icHW1eKCqjnAkT6tQik9fBWXZwkoKLmFVbbvfcT51a",
           |    "assets": [
           |        {
           |            "tokenId": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
           |            "index": 0,
           |            "amount": 1000,
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

  def orderV2Box =
    io.circe.parser
      .decode[Output](
        """
          |{
          |    "boxId": "66bd0bbeb11cc21ebb5648c4248771f82416ba88ca72fd992fa56d2f89717d8f",
          |    "transactionId": "946478bc6197984ce5c7dfecb1ce0759dc3f9102a542f8309939f4951201bd44",
          |    "blockId": "5c964e45f7465b70f04a6275d4cdfb93099c25a4e6123f05ce87d1603bc0953a",
          |    "value": 32478137,
          |    "index": 0,
          |    "globalIndex": 23320629,
          |    "creationHeight": 508928,
          |    "settlementHeight": 868391,
          |    "ergoTree": "19a8041804000e20010101010101010101010101010101010101010101010101010101010101010104c80f04d00f0100040404080402040004040400040606010104000e2000000000000000000000000000000000000000000000000000000000000000000e240008cd02ddbe95b7f88d47bd8c2db823cc5dd1be69a650556a44d4c15ac65e1d3e34324c05060580dac409050a0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005160100d804d601b2a4730000d6027301d6037302d6047303d1ec730495ed93b1a4730593b1db630872017306d80ad605db63087201d606b2a5730700d607b2db63087206730800d6088c720702d6097e720806d60ab27205730900d60b7e8c720a0206d60c7e8cb2db6308a7730a000206d60d7e8cb27205730b000206d60e9a7209730cedededededed938cb27205730d0001730e93c27206730f938c72070172029272087310927ec1720606997ec1a7069d9c72097e7311067e73120695938c720a017202909c9c720b720c7e7203069c720e9a9c720d7e7204069c720c7e720306909c9c720d720c7e7203069c720e9a9c720b7e7204069c720c7e72030690b0ada5d9010f639593c2720f7313c1720f73147315d9010f599a8c720f018c720f0273167317",
          |    "address": "GPZDFAnF3dSB9E6665Pi1ucYQ98UWzRYPfvkDrPQMcUcg7JvbggYMibCnp1Bvga8nePyLv8kAK62wfsV9zMQAP6ucV9HVUNBuXckyhB2BifPaRxeg5HALZnkTNTs4P6dvT7zPyd2a97iB3hba9WjkPRgKvJNVBsn7N24TKSwBpjGLtsTZP7KbD1RJYAWH1XsqBbvzTkSGeSEAEG1xEneQeikediJoNy6Kiij4ddGfAcJd1S3N6K7MpuhXVeVcAfAhXuLMQpuHSSinFvUTPA3cGDetahrJJ3oxXgDCiG2EQEoYVBsh7CBA37tq49mEpW2G7UPWFJieXv7cWxEEgreQnUYGKLgJgKUJVbeokGFPg87ReqdnMNnv3w5A1yGqucZeHpiLyWTkfqbDAQAPBwh8MaL2PbHm3bP8WmzBH5UVMM8ipMFrJiFrPGWvoN95WybGYUbTUJumcp9WAVL45kfAwDNvRrRa3p7Hu4FGxR1trFPiYkoujMiZyoiRDjQXAX8QBFChLd4JVwHZxmW48pkzWeNm9FPcrNejtr6STn3WTVPLt6GDa5rLL86RYtCjrWd4TKWKK2bJrKpa42r4PvzzeYK97HLxuFYyi3fTchqg5wd5yeSBpTTNaELvVUZhPuYaZGYd3JmiLYPebr2C7PdmhudFaA1zz9LU3oYZa1Dbt8vfGD4Rr4oukV2NtCcgUrrFjcN9XTXtHZWGu22E1TYzNbJ1Fr7hBFukdACCHtogUX41gzbpNtN7ba19kDa1JDGVvrvwRwv9CcGpmb342t459sFrzyQ1qsicL7gwQjnUvKUV5n4wgWajGfcmLG5JC6LAwUZiGgjqZSnJ3SG4bSXUictNcZ1k8CGEqaoC1VYbyjft9cuvH1YdNmYWdKMzydyLtcRomw7hm8Dpcu7Z8tWXtfNUTCht8YLdqMnctutBMzbz3jdW1DcBtNeqJQgz6t5HZC9wYAXvL9P6GcmhvHs3NhaKpT61p5bw8LXJmZdYLqyuG7nfs4ZANHYSSMFWBygZrXxerP6u7ibfRQanVWSNkHYJCBipqZGSfK7kZzT7HCmbioVBgDwYSLCwXdbX8Rhc8QGopUQSau3nqixeYns55gMBK1WAwtobXZtAzfpVH4ytofRuUrUtEumLQJzJuwT7P66WjYRLs95NfmnmQLvVrKNg5jNcPybjKDZS8VLATTm8cALygHvFg6WuUZeVusBEKLthYG2CdYrfqpFS9dmumGT1pFmssUYtanUqd8trDFCwDvNMWTLmTS4WeuF1QbGtfZ4f91P95GMpUxCUVJLRGWZX5imu4Vid",
          |    "assets": [
          |        {
          |            "tokenId": "30974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e",
          |            "index": 0,
          |            "amount": 10,
          |            "name": "WT_ADA",
          |            "decimals": 8,
          |            "type": "EIP-004"
          |        }
          |    ],
          |    "additionalRegisters": {},
          |    "spentTransactionId": "391cf4060101b267a503334570688ba5f6219c2b31162abc41260e725b5ac6a9",
          |    "mainChain": true
          |}
          |""".stripMargin
      )
      .toOption
      .get

  def boxSampleSwapXIsSpectrum =
    io.circe.parser
      .decode[Output](
        s"""
           |{
           |    "boxId": "cd29cc599a8a53d28504f2752fc075286359d6b7f1e82582b7e6cd45468a6b29",
           |    "transactionId": "52cdef65d1a93e29774904b5457577e7ec351a6fded878f5ee63733020865495",
           |    "blockId": "274b42d256323aa61a1735f55ca15071a789704b9c66d5daba19151f4199e7e3",
           |    "value": 7260000,
           |    "index": 0,
           |    "globalIndex": 20758110,
           |    "creationHeight": 825489,
           |    "settlementHeight": 825491,
           |    "ergoTree": "19c4052204000e20010101010101010101010101010101010101010101010101010101010101010105f01504d00f0e200202020202020202020202020202020202020202020202020202020202020202040004c80f0100040404080402040001010408040405f015040606010104000e2000000000000000000000000000000000000000000000000000000000000000000e20030303030303030303030303030303030303030303030303030303030303030305c00c0101010105160518060100040201010e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005e2f85e0100d809d601b2a4730000d6027301d6037302d6047303d6057304d606db6308a7d607b27206730500d6088c720702d6097306d1ec730795ed92b1a4730893b1db630872017309d80dd60adb63087201d60bb2a5730a00d60cdb6308720bd60db2720c730b00d60e8c720d02d60f95730c9d9c7e720406997e720e067e7203067e730d067e720e06d610b2720a730e00d6117e8c72100206d6127206d6137207d6147e95948c72130172057208997208730f06d6157e8cb2720a7310000206d6169a720f7311edededededed938cb2720a73120001731393c2720b7314938c720d01720292720e73159573167317d801d617997e7203069d9c720f7e7318067e73190695917217731ad801d618b2720c731b00ed938c7218017205927e8c721802067217731c95938c7210017202909c9c721172147e7209069c72169a9c72157e7204069c72147e720906909c9c721572147e7209069c72169a9c72117e7204069c72147e72090690b0ada5d90117639593c27217731dc17217731e731fd90117599a8c7217018c72170273207321",
           |    "address": "ynjTGtEt9xdN4LPAreFLJVwwdzZgP1tG5jqMBtvhF1Rr7bsMjBioWPLamCZBkytNPRuKCvzFq2Ghr76X2wVvzwPEiGUDEyykJDTbrChB8iNS8xARPwijRGZGymCG7E3xMNn7tCX2yZU3qT5a4ngG8RCJrvVjTef2kuU4jKARdXY8xYEJUDLBsUnssoT54ma2N3xzQRi4cAhA5Ap5AbM5YxV63GaBewRuEshjrTJH4dLDx6ApyVNmpVRwaZUuiLHFvdUtqcFsyakNyACaWTiJfPCWygFsBznUnMmfKB84eRjdmdL1jPB8TF4Heus2Hh82KVThdfWxAe1P5TVkovNUgLw7t4DyuTh6Mr9sMxzTPiidDriv1mKhrDGGru6w7uFhJ5fKZFQHskZ9ei2ybz6D9Vmz9tuUtcEWUGn5Jh8hjbYVHe9ckCCT2Sh56bV5DEx4hZ7Egnprt6ADdMU1sLoN4N5youbByQsZBmDxrbRmGEyMQaubBFJfEKGGk8EUjENvXgCiZ14iXhiyWQaoLFP1QzNrAJHeCtYNLq1XTAH1j2ciMw8CChJELXeAAd9nvUheizsg6kUqzsraCscg6FrEntCiC9ZtPkvcD8KEmtzj5bUDh6WNzMkGUuMtWVr3qto1Eu94DQDkzSP6dCZvRHRjribkHeYTQvhyjREEUzpS2a5zsAY1UxBsRJt45pcHFwanfYTg2kAJnhTCm9rE13kgyHcgZGnKpKwCJxEoYLXUiMRZ6icHW1eKCqjnAkT6tQik9fBWXZwkoKLmFVbbvfcT51a",
           |    "assets": [
           |        {
           |            "tokenId": "0202020202020202020202020202020202020202020202020202020202020202",
           |            "index": 0,
           |            "amount": 1000,
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

  def boxSampleSwap =
    io.circe.parser
      .decode[Output](
        s"""
           |{
           |    "boxId": "cd29cc599a8a53d28504f2752fc075286359d6b7f1e82582b7e6cd45468a6b29",
           |    "transactionId": "52cdef65d1a93e29774904b5457577e7ec351a6fded878f5ee63733020865495",
           |    "blockId": "274b42d256323aa61a1735f55ca15071a789704b9c66d5daba19151f4199e7e3",
           |    "value": 7260000,
           |    "index": 0,
           |    "globalIndex": 20758110,
           |    "creationHeight": 825489,
           |    "settlementHeight": 825491,
           |    "ergoTree": "19c4052204000e20010101010101010101010101010101010101010101010101010101010101010105f01504d00f0e200202020202020202020202020202020202020202020202020202020202020202040004c80f0100040404080402040001010408040405f015040606010104000e2000000000000000000000000000000000000000000000000000000000000000000e20030303030303030303030303030303030303030303030303030303030303030305c00c0101010105160518060100040201010e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005e2f85e0100d809d601b2a4730000d6027301d6037302d6047303d6057304d606db6308a7d607b27206730500d6088c720702d6097306d1ec730795ed92b1a4730893b1db630872017309d80dd60adb63087201d60bb2a5730a00d60cdb6308720bd60db2720c730b00d60e8c720d02d60f95730c9d9c7e720406997e720e067e7203067e730d067e720e06d610b2720a730e00d6117e8c72100206d6127206d6137207d6147e95948c72130172057208997208730f06d6157e8cb2720a7310000206d6169a720f7311edededededed938cb2720a73120001731393c2720b7314938c720d01720292720e73159573167317d801d617997e7203069d9c720f7e7318067e73190695917217731ad801d618b2720c731b00ed938c7218017205927e8c721802067217731c95938c7210017202909c9c721172147e7209069c72169a9c72157e7204069c72147e720906909c9c721572147e7209069c72169a9c72117e7204069c72147e72090690b0ada5d90117639593c27217731dc17217731e731fd90117599a8c7217018c72170273207321",
           |    "address": "ynjTGtEt9xdN4LPAreFLJVwwdzZgP1tG5jqMBtvhF1Rr7bsMjBioWPLamCZBkytNPRuKCvzFq2Ghr76X2wVvzwPEiGUDEyykJDTbrChB8iNS8xARPwijRGZGymCG7E3xMNn7tCX2yZU3qT5a4ngG8RCJrvVjTef2kuU4jKARdXY8xYEJUDLBsUnssoT54ma2N3xzQRi4cAhA5Ap5AbM5YxV63GaBewRuEshjrTJH4dLDx6ApyVNmpVRwaZUuiLHFvdUtqcFsyakNyACaWTiJfPCWygFsBznUnMmfKB84eRjdmdL1jPB8TF4Heus2Hh82KVThdfWxAe1P5TVkovNUgLw7t4DyuTh6Mr9sMxzTPiidDriv1mKhrDGGru6w7uFhJ5fKZFQHskZ9ei2ybz6D9Vmz9tuUtcEWUGn5Jh8hjbYVHe9ckCCT2Sh56bV5DEx4hZ7Egnprt6ADdMU1sLoN4N5youbByQsZBmDxrbRmGEyMQaubBFJfEKGGk8EUjENvXgCiZ14iXhiyWQaoLFP1QzNrAJHeCtYNLq1XTAH1j2ciMw8CChJELXeAAd9nvUheizsg6kUqzsraCscg6FrEntCiC9ZtPkvcD8KEmtzj5bUDh6WNzMkGUuMtWVr3qto1Eu94DQDkzSP6dCZvRHRjribkHeYTQvhyjREEUzpS2a5zsAY1UxBsRJt45pcHFwanfYTg2kAJnhTCm9rE13kgyHcgZGnKpKwCJxEoYLXUiMRZ6icHW1eKCqjnAkT6tQik9fBWXZwkoKLmFVbbvfcT51a",
           |    "assets": [
           |        {
           |            "tokenId": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
           |            "index": 0,
           |            "amount": 1000,
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

  def boxSampleRedeem =
    io.circe.parser
      .decode[Output](
        s"""
           |{
           |    "boxId": "cd29cc599a8a53d28504f2752fc075286359d6b7f1e82582b7e6cd45468a6b29",
           |    "transactionId": "52cdef65d1a93e29774904b5457577e7ec351a6fded878f5ee63733020865495",
           |    "blockId": "274b42d256323aa61a1735f55ca15071a789704b9c66d5daba19151f4199e7e3",
           |    "value": 7260000,
           |    "index": 0,
           |    "globalIndex": 20758110,
           |    "creationHeight": 825489,
           |    "settlementHeight": 825491,
           |    "ergoTree": "19c40314040001000404040804020400040404020406040005feffffffffffffffff01040204000e2000000000000000000000000000000000000000000000000000000000000000000e2003030303030303030303030303030303030303030303030303030303030303030e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005e2f85e0100d801d601b2a4730000d1ec730195ed92b1a4730293b1db630872017303d809d602db63087201d603b2a5730400d604db63087203d605b27204730500d606b27202730600d607b27204730700d608b27202730800d6097e8cb2db6308a77309000206d60a7e99730a8cb27202730b000206edededededed938cb27202730c0001730d93c27203730e938c7205018c720601938c7207018c720801927e8c720502069d9c72097e8c72060206720a927e8c720702069d9c72097e8c72080206720a90b0ada5d9010b639593c2720b730fc1720b73107311d9010b599a8c720b018c720b0273127313",
           |    "address": "ynjTGtEt9xdN4LPAreFLJVwwdzZgP1tG5jqMBtvhF1Rr7bsMjBioWPLamCZBkytNPRuKCvzFq2Ghr76X2wVvzwPEiGUDEyykJDTbrChB8iNS8xARPwijRGZGymCG7E3xMNn7tCX2yZU3qT5a4ngG8RCJrvVjTef2kuU4jKARdXY8xYEJUDLBsUnssoT54ma2N3xzQRi4cAhA5Ap5AbM5YxV63GaBewRuEshjrTJH4dLDx6ApyVNmpVRwaZUuiLHFvdUtqcFsyakNyACaWTiJfPCWygFsBznUnMmfKB84eRjdmdL1jPB8TF4Heus2Hh82KVThdfWxAe1P5TVkovNUgLw7t4DyuTh6Mr9sMxzTPiidDriv1mKhrDGGru6w7uFhJ5fKZFQHskZ9ei2ybz6D9Vmz9tuUtcEWUGn5Jh8hjbYVHe9ckCCT2Sh56bV5DEx4hZ7Egnprt6ADdMU1sLoN4N5youbByQsZBmDxrbRmGEyMQaubBFJfEKGGk8EUjENvXgCiZ14iXhiyWQaoLFP1QzNrAJHeCtYNLq1XTAH1j2ciMw8CChJELXeAAd9nvUheizsg6kUqzsraCscg6FrEntCiC9ZtPkvcD8KEmtzj5bUDh6WNzMkGUuMtWVr3qto1Eu94DQDkzSP6dCZvRHRjribkHeYTQvhyjREEUzpS2a5zsAY1UxBsRJt45pcHFwanfYTg2kAJnhTCm9rE13kgyHcgZGnKpKwCJxEoYLXUiMRZ6icHW1eKCqjnAkT6tQik9fBWXZwkoKLmFVbbvfcT51a",
           |    "assets": [
           |        {
           |            "tokenId": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
           |            "index": 0,
           |            "amount": 1000,
           |            "name": "SigUSD",
           |            "decimals": 2,
           |            "type": "EIP-004"
           |        },
           |        {
           |            "tokenId": "02faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
           |            "index": 0,
           |            "amount": 1001,
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

  def boxSampleDepositXIsSpectrum =
    io.circe.parser
      .decode[Output](
        s"""
           |{
           |    "boxId": "cd29cc599a8a53d28504f2752fc075286359d6b7f1e82582b7e6cd45468a6b29",
           |    "transactionId": "52cdef65d1a93e29774904b5457577e7ec351a6fded878f5ee63733020865495",
           |    "blockId": "274b42d256323aa61a1735f55ca15071a789704b9c66d5daba19151f4199e7e3",
           |    "value": 7260000,
           |    "index": 0,
           |    "globalIndex": 20758110,
           |    "creationHeight": 825489,
           |    "settlementHeight": 825491,
           |    "ergoTree": "19f2041d08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04000404040804020400040205feffffffffffffffff010404010005f60104020406010105f601040004000e2000000000000000000000000000000000000000000000000000000000000000000404040204040402010101000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005e2f85e0100d802d6017300d602b2a4730100eb027201d195ed92b1a4730293b1db630872027303d80fd603db63087202d604b2a5730400d605db6308a7d6068cb2720573050002d607b27203730600d6087e9973078c72070206d609b27203730800d60a7e8c72090206d60b9d9c7e957309997206730a7206067208720ad60c8cb27205730b0002d60db27203730c00d60e7e8c720d0206d60f9d9c7e95730d99720c730e720c067208720ed610db63087204d611b27210730f00edededededed938cb2720373100001731193c27204d0720192c17204c1a795ed8f720b720f93b172107312d801d612b27210731300ed938c7212018c720d01927e8c721202069d9c99720f720b720e720895ed91720b720f93b172107314d801d612b27210731500ed938c7212018c720901927e8c721202069d9c99720b720f720a72089593720b720f73167317938c7211018c720701927e8c72110206a1720b720f90b0ada5d90112639593c272127318c172127319731ad90112599a8c7212018c721202731b731c",
           |    "address": "ynjTGtEt9xdN4LPAreFLJVwwdzZgP1tG5jqMBtvhF1Rr7bsMjBioWPLamCZBkytNPRuKCvzFq2Ghr76X2wVvzwPEiGUDEyykJDTbrChB8iNS8xARPwijRGZGymCG7E3xMNn7tCX2yZU3qT5a4ngG8RCJrvVjTef2kuU4jKARdXY8xYEJUDLBsUnssoT54ma2N3xzQRi4cAhA5Ap5AbM5YxV63GaBewRuEshjrTJH4dLDx6ApyVNmpVRwaZUuiLHFvdUtqcFsyakNyACaWTiJfPCWygFsBznUnMmfKB84eRjdmdL1jPB8TF4Heus2Hh82KVThdfWxAe1P5TVkovNUgLw7t4DyuTh6Mr9sMxzTPiidDriv1mKhrDGGru6w7uFhJ5fKZFQHskZ9ei2ybz6D9Vmz9tuUtcEWUGn5Jh8hjbYVHe9ckCCT2Sh56bV5DEx4hZ7Egnprt6ADdMU1sLoN4N5youbByQsZBmDxrbRmGEyMQaubBFJfEKGGk8EUjENvXgCiZ14iXhiyWQaoLFP1QzNrAJHeCtYNLq1XTAH1j2ciMw8CChJELXeAAd9nvUheizsg6kUqzsraCscg6FrEntCiC9ZtPkvcD8KEmtzj5bUDh6WNzMkGUuMtWVr3qto1Eu94DQDkzSP6dCZvRHRjribkHeYTQvhyjREEUzpS2a5zsAY1UxBsRJt45pcHFwanfYTg2kAJnhTCm9rE13kgyHcgZGnKpKwCJxEoYLXUiMRZ6icHW1eKCqjnAkT6tQik9fBWXZwkoKLmFVbbvfcT51a",
           |    "assets": [
           |        {
           |            "tokenId": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
           |            "index": 0,
           |            "amount": 1000,
           |            "name": "SigUSD",
           |            "decimals": 2,
           |            "type": "EIP-004"
           |        },
           |        {
           |            "tokenId": "01faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
           |            "index": 0,
           |            "amount": 1001,
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

  def boxSampleDepositYIsSpectrum =
    io.circe.parser
      .decode[Output](
        s"""
           |{
           |    "boxId": "cd29cc599a8a53d28504f2752fc075286359d6b7f1e82582b7e6cd45468a6b29",
           |    "transactionId": "52cdef65d1a93e29774904b5457577e7ec351a6fded878f5ee63733020865495",
           |    "blockId": "274b42d256323aa61a1735f55ca15071a789704b9c66d5daba19151f4199e7e3",
           |    "value": 7260000,
           |    "index": 0,
           |    "globalIndex": 20758110,
           |    "creationHeight": 825489,
           |    "settlementHeight": 825491,
           |    "ergoTree": "19f2041d08cd02217daf90deb73bdf8b6709bb42093fdfaff6573fd47b630e2d3fdd4a8193a74d04000404040804020400040205feffffffffffffffff010404010105f60104020406010005f601040004000e2000000000000000000000000000000000000000000000000000000000000000000404040204040402010101000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005e2f85e0100d802d6017300d602b2a4730100eb027201d195ed92b1a4730293b1db630872027303d80fd603db63087202d604b2a5730400d605db6308a7d6068cb2720573050002d607b27203730600d6087e9973078c72070206d609b27203730800d60a7e8c72090206d60b9d9c7e957309997206730a7206067208720ad60c8cb27205730b0002d60db27203730c00d60e7e8c720d0206d60f9d9c7e95730d99720c730e720c067208720ed610db63087204d611b27210730f00edededededed938cb2720373100001731193c27204d0720192c17204c1a795ed8f720b720f93b172107312d801d612b27210731300ed938c7212018c720d01927e8c721202069d9c99720f720b720e720895ed91720b720f93b172107314d801d612b27210731500ed938c7212018c720901927e8c721202069d9c99720b720f720a72089593720b720f73167317938c7211018c720701927e8c72110206a1720b720f90b0ada5d90112639593c272127318c172127319731ad90112599a8c7212018c721202731b731c",
           |    "address": "ynjTGtEt9xdN4LPAreFLJVwwdzZgP1tG5jqMBtvhF1Rr7bsMjBioWPLamCZBkytNPRuKCvzFq2Ghr76X2wVvzwPEiGUDEyykJDTbrChB8iNS8xARPwijRGZGymCG7E3xMNn7tCX2yZU3qT5a4ngG8RCJrvVjTef2kuU4jKARdXY8xYEJUDLBsUnssoT54ma2N3xzQRi4cAhA5Ap5AbM5YxV63GaBewRuEshjrTJH4dLDx6ApyVNmpVRwaZUuiLHFvdUtqcFsyakNyACaWTiJfPCWygFsBznUnMmfKB84eRjdmdL1jPB8TF4Heus2Hh82KVThdfWxAe1P5TVkovNUgLw7t4DyuTh6Mr9sMxzTPiidDriv1mKhrDGGru6w7uFhJ5fKZFQHskZ9ei2ybz6D9Vmz9tuUtcEWUGn5Jh8hjbYVHe9ckCCT2Sh56bV5DEx4hZ7Egnprt6ADdMU1sLoN4N5youbByQsZBmDxrbRmGEyMQaubBFJfEKGGk8EUjENvXgCiZ14iXhiyWQaoLFP1QzNrAJHeCtYNLq1XTAH1j2ciMw8CChJELXeAAd9nvUheizsg6kUqzsraCscg6FrEntCiC9ZtPkvcD8KEmtzj5bUDh6WNzMkGUuMtWVr3qto1Eu94DQDkzSP6dCZvRHRjribkHeYTQvhyjREEUzpS2a5zsAY1UxBsRJt45pcHFwanfYTg2kAJnhTCm9rE13kgyHcgZGnKpKwCJxEoYLXUiMRZ6icHW1eKCqjnAkT6tQik9fBWXZwkoKLmFVbbvfcT51a",
           |    "assets": [
           |        {
           |            "tokenId": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
           |            "index": 0,
           |            "amount": 1000,
           |            "name": "SigUSD",
           |            "decimals": 2,
           |            "type": "EIP-004"
           |        },
           |        {
           |            "tokenId": "01faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
           |            "index": 0,
           |            "amount": 1001,
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
