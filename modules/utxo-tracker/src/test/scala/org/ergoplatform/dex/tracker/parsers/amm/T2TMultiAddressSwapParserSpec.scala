package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.IO
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder}
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMOrder.{Swap, SwapMultiAddress}
import org.ergoplatform.dex.domain.amm.{PoolId, SwapParams}
import org.ergoplatform.ergo.domain.{BoxAsset, Output}
import org.ergoplatform.ergo._
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class T2TMultiAddressSwapParserSpec
  extends AnyPropSpec
  with should.Matchers
  with ScalaCheckPropertyChecks
  with CatsPlatform {

  property("Swap multi address order parsing with p2s address") {
    val treeP2S =
      "19a8071804000e20ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b04c80f04d00f0100040404080402040004040400040606010104000e20f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc7810ea50319a2031308cd0385d4617286f1a77d8f02f2294cadb5ca47112c3fa717ccf74c1da7b68ffc329a04000e20ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b04c80f04d00f040404080402040004040400040606010104000e20f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc7810582d2b39ffa0105cad2b3b294e35e058080a0f6f4acdbe01b0100d805d6017300d602b2a4730100d6037302d6047303d6057304eb027201d195ed93b1a4730593b1db630872027306d80ad606db63087202d607b2a5730700d608b2db63087207730800d6098c720802d60a7e720906d60bb27206730900d60c7e8c720b0206d60d7e8cb2db6308a7730a000206d60e7e8cb27206730b000206d60f9a720a730cededededed938cb27206730d0001730e93c27207d07201938c7208017203927209730f927ec1720706997ec1a7069d9c720a7e7310067e73110695938c720b017203909c9c720c720d7e7204069c720f9a9c720e7e7205069c720d7e720406909c9c720e720d7e7204069c720f9a9c720c7e7205069c720d7e720406731205c00c050e05160e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005000100d804d601b2a4730000d6027301d6037302d6047303d1ec730495ed93b1a4730593b1db630872017306d80ad605db63087201d606b2a5730700d607b2db63087206730800d6088c720702d6097e720806d60ab27205730900d60b7e8c720a0206d60c7e8cb2db6308a7730a000206d60d7e8cb27205730b000206d60e9a7209730cedededededed938cb27205730d0001730e93c27206730f938c72070172029272087310927ec1720606997ec1a7069d9c72097e7311067e73120695938c720a017202909c9c720b720c7e7203069c720e9a9c720d7e7204069c720c7e720306909c9c720d720c7e7203069c720e9a9c720b7e7204069c720c7e72030690b0ada5d9010f639593c2720f7313c1720f73147315d9010f599a8c720f018c720f0273167317"
    val res = parser.swap(boxSample(treeP2S)).unsafeRunSync()
    res shouldBe result(treeP2S, SErgoTree.fromBytes(p2sAddr.script.bytes))

  }

  property("Swap multi address order parsing with p2pk address") {
    val treeP2PK =
      "19a6041804000e20ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b04c80f04d00f0100040404080402040004040400040606010104000e20f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc7810e240008cd026329ce69c270a944ff7c6ba6eef00b2e4fd9e5f32f8ebac8f45862f2cadd6be205c00c050e05160e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a573040500050005000100d804d601b2a4730000d6027301d6037302d6047303d1ec730495ed93b1a4730593b1db630872017306d80ad605db63087201d606b2a5730700d607b2db63087206730800d6088c720702d6097e720806d60ab27205730900d60b7e8c720a0206d60c7e8cb2db6308a7730a000206d60d7e8cb27205730b000206d60e9a7209730cedededededed938cb27205730d0001730e93c27206730f938c72070172029272087310927ec1720606997ec1a7069d9c72097e7311067e73120695938c720a017202909c9c720b720c7e7203069c720e9a9c720d7e7204069c720c7e720306909c9c720d720c7e7203069c720e9a9c720b7e7204069c720c7e72030690b0ada5d9010f639593c2720f7313c1720f73147315d9010f599a8c720f018c720f0273167317"
    val res = parser.swap(boxSample(treeP2PK)).unsafeRunSync()
    res shouldBe result(treeP2PK, SErgoTree.fromBytes(p2pkAddr.script.bytes))

  }

  implicit val e: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  val p2sAddr: ErgoAddress = e
    .fromString(
      "H3AA3N1iey8DqJarK13ccAQBa1ZWmoBkmacJcNgR2zk83bSdxrxps32TPsDnEuyANMmsqrRn6XPvtNbRrVA1WkztQQLmFqikouURGsJux5LouDFfU1p7aPxmAr5p1RZefPvZNVhtrmhj5ELM9QzdojLaG1Gg6eDyuEQqfHNkPhjm2nX6EK5QRk7PWXT7ichMqr3vDKJqioP92P94tdL7SYQWTb7Uvfaw6E6xtUX6jsuiRyFC4fgVtKmvAxKKSmdxhUvmtvaEjkkSdmdt7pd57e8FhhzkYiojPb23CxQ3dK2prmdzCCFeRWuS4bxJeysU5LjcRvbU7ERKq28bKUR5eU5DL2cGwvkBfAHUXiBAA3GSq1Sp1P3NTciE1VnL1US7H4T7q4MMeBE7NDNk1MKamdFU8mrVSFPPs57jALA8tAeH5uXja27hZjM3PufE6eHFQ2mnX64LU6oYNyLbzTQMvQUQpA4DyUcgD7Dp47vzcTV5kcEp3XEyubESLcD4veCUGRweEEnrcW3PkbPaQdiMcuTE3N4UshDw5uXatFcWQo9QPxVKpwXx7R7JbqHM6PYXjkZDS"
    )
    .get

  val p2pkAddr: ErgoAddress = e
    .fromString(
      "9fGjWT89UgC6KYqUcLiDG8NGtAF9rEznEcryYqj9Z4W42PZRjas"
    )
    .get

  def parser = T2TCFMMOrdersParserMultiAddress.make[IO]

  def boxSample(tree: String) =
    io.circe.parser
      .decode[Output](
        s"""
            |{
            |    "boxId": "e419674609fe037d98d07e9c7074b3ad25f2c4e69a9bf844c389117a332fa87d",
            |    "transactionId": "0203eb80c9c8ebe09bdc466c779eb687e3f6b6f8f0c176f01a61fc10aca6cdbd",
            |    "blockId": "ba6012b63585a48610ef9355bc0337bfd0c523841a1871a2e841c9b5e388f191",
            |    "value": 16050000,
            |    "index": 0,
            |    "globalIndex": 5972866,
            |    "creationHeight": 508928,
            |    "settlementHeight": 547184,
            |    "ergoTree": "$tree",
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

  def result(tree: String, addr: SErgoTree) = Some(
    SwapMultiAddress(
      PoolId.fromStringUnsafe("f1fb942ebd039dc782fd9109acdb60aabea4dc7e75e9c813b6528c62692fc781"),
      0L,
      FixedTs,
      SwapParams[SErgoTree](
        AssetAmount(
          TokenId.fromStringUnsafe("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b"),
          1000000000
        ),
        AssetAmount(
          TokenId.fromStringUnsafe("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b"),
          800
        ),
        dexFeePerTokenNum   = 7,
        dexFeePerTokenDenom = 11,
        redeemer            = addr
      ),
      Output(
        BoxId("e419674609fe037d98d07e9c7074b3ad25f2c4e69a9bf844c389117a332fa87d"),
        TxId("0203eb80c9c8ebe09bdc466c779eb687e3f6b6f8f0c176f01a61fc10aca6cdbd"),
        16050000,
        0,
        508928,
        SErgoTree.unsafeFromString(tree),
        List(
          BoxAsset(
            TokenId.fromStringUnsafe("ef802b475c06189fdbf844153cdc1d449a5ba87cce13d11bb47b5a539f27f12b"),
            1000000000L
          )
        ),
        Map()
      )
    )
  )

}
