package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.{Clock, SyncIO}
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMOrder.SwapMultiAddress
import org.ergoplatform.dex.domain.amm.{CFMMOrder, PoolId, SwapParams}
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.{AMMType, ParserType}
import org.ergoplatform.ergo._
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class T2TMultiAddressSwapParserSpec
  extends AnyPropSpec
  with should.Matchers
  with ScalaCheckPropertyChecks
  with CatsPlatform {

  implicit val clock: Clock[SyncIO] = Clock.create

  property("Ergo tree from swaps should be deserializable ") {
    val treeP2Pk =
      "19a8041804000e20010101010101010101010101010101010101010101010101010101010101010104c80f04d00f0100040404080402040004040400040606010104000e2000000000000000000000000000000000000000000000000000000000000000000e240008cd02ddbe95b7f88d47bd8c2db823cc5dd1be69a650556a44d4c15ac65e1d3e34324c05060580dac409050a0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005160100d804d601b2a4730000d6027301d6037302d6047303d1ec730495ed93b1a4730593b1db630872017306d80ad605db63087201d606b2a5730700d607b2db63087206730800d6088c720702d6097e720806d60ab27205730900d60b7e8c720a0206d60c7e8cb2db6308a7730a000206d60d7e8cb27205730b000206d60e9a7209730cedededededed938cb27205730d0001730e93c27206730f938c72070172029272087310927ec1720606997ec1a7069d9c72097e7311067e73120695938c720a017202909c9c720b720c7e7203069c720e9a9c720d7e7204069c720c7e720306909c9c720d720c7e7203069c720e9a9c720b7e7204069c720c7e72030690b0ada5d9010f639593c2720f7313c1720f73147315d9010f599a8c720f018c720f0273167317"
    val order = parser.swap(boxSample(treeP2Pk)).unsafeRunSync().get match {
      case swap: SwapMultiAddress => swap
    }

    val `try` = ErgoTreeSerializer.default.deserialize(order.params.redeemer)

    `try` shouldBe `try`
  }

  property("Resulted ergo tree have to be deserializable") {
    val treeP2Pk =
      "19a8041804000e20010101010101010101010101010101010101010101010101010101010101010104c80f04d00f0100040404080402040004040400040606010104000e2000000000000000000000000000000000000000000000000000000000000000000e240008cd02ddbe95b7f88d47bd8c2db823cc5dd1be69a650556a44d4c15ac65e1d3e34324c05060580dac409050a0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005160100d804d601b2a4730000d6027301d6037302d6047303d1ec730495ed93b1a4730593b1db630872017306d80ad605db63087201d606b2a5730700d607b2db63087206730800d6088c720702d6097e720806d60ab27205730900d60b7e8c720a0206d60c7e8cb2db6308a7730a000206d60d7e8cb27205730b000206d60e9a7209730cedededededed938cb27205730d0001730e93c27206730f938c72070172029272087310927ec1720606997ec1a7069d9c72097e7311067e73120695938c720a017202909c9c720b720c7e7203069c720e9a9c720d7e7204069c720c7e720306909c9c720d720c7e7203069c720e9a9c720b7e7204069c720c7e72030690b0ada5d9010f639593c2720f7313c1720f73147315d9010f599a8c720f018c720f0273167317"
    val order = parser.swap(boxSample(treeP2Pk)).unsafeRunSync().get match {
      case swap: SwapMultiAddress => swap
    }

    order.params.redeemer shouldEqual ErgoTreeSerializer.default.serialize(p2pk.script)
  }

  property("T2T parser should parse swap multi address order correct") {

    def redeemerSwapBuy = SErgoTree.unsafeFromString(
      "19b1031208cd033d6ab05cfb8a65938e116cb863cad577e560bb8e110113bf395fbe98649dbb59040004040406040204000404040005" +
      "feffffffffffffffff01040204000e209916d75132593c8b07fe18bd8d583bda1652eed7565cf41a4738ddd90fc992ec0580b6dc05" +
      "0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d1" +
      "92a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573" +
      "0405000500058092f4010100d802d6017300d602b2a4730100eb027201d195ed93b1a4730293b1db630872027303d806d603db6308" +
      "7202d604b2a5730400d605b2db63087204730500d606b27203730600d6077e8cb2db6308a77307000206d6087e9973088cb2720373" +
      "09000206ededededed938cb27203730a0001730b93c27204d07201938c7205018c720601927e9a99c17204c1a7730c069d9c72077e" +
      "c17202067208927e8c720502069d9c72077e8c72060206720890b0ada5d90109639593c27209730dc17209730e730fd90109599a8c" +
      "7209018c72090273107311"
    )

    val tree = "101804000e20ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b04c80f04d00f01000404040" +
      "80402040004040400040606010104000e20f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc7810eb40319b" +
      "1031208cd033d6ab05cfb8a65938e116cb863cad577e560bb8e110113bf395fbe98649dbb59040004040406040204000404040005fef" +
      "fffffffffffffff01040204000e209916d75132593c8b07fe18bd8d583bda1652eed7565cf41a4738ddd90fc992ec0580b6dc050e691" +
      "005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8" +
      "cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050" +
      "0058092f4010100d802d6017300d602b2a4730100eb027201d195ed93b1a4730293b1db630872027303d806d603db63087202d604b2a" +
      "5730400d605b2db63087204730500d606b27203730600d6077e8cb2db6308a77307000206d6087e9973088cb272037309000206edede" +
      "deded938cb27203730a0001730b93c27204d07201938c7205018c720601927e9a99c17204c1a7730c069d9c72077ec17202067208927" +
      "e8c720502069d9c72077e8c72060206720890b0ada5d90109639593c27209730dc17209730e730fd90109599a8c7209018c720902731" +
      "0731105060580dac409050a0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d95" +
      "9f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a573010074730273038" +
      "30108cdeeac93b1a57304050005000580dac4090100d804d601b2a4730000d6027301d6037302d6047303d1ec730495ed93b1a473059" +
      "3b1db630872017306d80ad605db63087201d606b2a5730700d607b2db63087206730800d6088c720702d6097e720806d60ab27205730" +
      "900d60b7e8c720a0206d60c7e8cb2db6308a7730a000206d60d7e8cb27205730b000206d60e9a7209730cedededededed938cb272057" +
      "30d0001730e93c27206730f938c72070172029272087310927ec1720606997ec1a7069d9c72097e7311067e73120695938c720a01720" +
      "2909c9c720b720c7e7203069c720e9a9c720d7e7204069c720c7e720306909c9c720d720c7e7203069c720e9a9c720b7e7204069c720" +
      "c7e72030690b0ada5d9010f639593c2720f7313c1720f73147315d9010f599a8c720f018c720f0273167317"

    val order = parser.swap(boxSample(tree)).unsafeRunSync().get

    val expectedSwapOrder = CFMMOrder.SwapMultiAddress(
      PoolId.fromStringUnsafe("f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc781"),
      10000000L,
      order.timestamp,
      SwapParams(
        AssetAmount(
          TokenId.fromStringUnsafe("30974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e"),
          10L
        ),
        AssetAmount(
          TokenId.fromStringUnsafe("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b"),
          3L
        ),
        10000000L,
        5L,
        redeemerSwapBuy
      ),
      boxSample(tree)
    )

    order shouldBe expectedSwapOrder
  }

  implicit val e: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  def p2pk: ErgoAddress = e.fromString("9gCigPc9cZNRhKgbgdmTkVxo1ZKgw79G8DvLjCcYWAvEF3XRUKy").get

  def parser: CFMMOrdersParser[AMMType.T2T_CFMM, ParserType.MultiAddress, SyncIO] =
    T2TCFMMOrdersParserMultiAddress.make[SyncIO]

  def boxSample(tree: String) =
    io.circe.parser
      .decode[Output](
        s"""
            |{
            |    "boxId": "66bd0bbeb11cc21ebb5648c4248771f82416ba88ca72fd992fa56d2f89717d8f",
            |    "transactionId": "946478bc6197984ce5c7dfecb1ce0759dc3f9102a542f8309939f4951201bd44",
            |    "blockId": "5c964e45f7465b70f04a6275d4cdfb93099c25a4e6123f05ce87d1603bc0953a",
            |    "value": 32478137,
            |    "index": 0,
            |    "globalIndex": 23320629,
            |    "creationHeight": 508928,
            |    "settlementHeight": 868391,
            |    "ergoTree": "$tree",
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
}
