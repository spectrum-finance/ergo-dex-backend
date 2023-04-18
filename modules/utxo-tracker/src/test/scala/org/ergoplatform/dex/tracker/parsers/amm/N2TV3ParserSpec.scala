package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.{Clock, SyncIO}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.{AMMType, ParserVersion}
import org.ergoplatform.dex.tracker.parsers.amm.v3.N2TOrdersV3Parser
import org.ergoplatform.ergo._
import org.ergoplatform.ergo.domain.Output
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class N2TV3ParserSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with CatsPlatform {

  implicit val clock: Clock[SyncIO] = Clock.create

  implicit val e: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  def parser: CFMMOrdersParser[AMMType.N2T_CFMM, ParserVersion.V3, SyncIO] =
    N2TOrdersV3Parser.make[SyncIO](
      TokenId.fromStringUnsafe("003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0")
    )

  property("Parse deposit correct") {
    val deposit = parser.deposit(depositV3).unsafeRunSync().get.asInstanceOf[DepositTokenFee]

    val expected = CFMMOrder.DepositTokenFee(
      PoolId.fromStringUnsafe("9916d75132593c8b07fe18bd8d583bda1652eed7565cf41a4738ddd90fc992ec"),
      2000000,
      deposit.timestamp,
      DepositParams(
        AssetAmount(
          TokenId.fromStringUnsafe("0000000000000000000000000000000000000000000000000000000000000000"),
          12263164
        ),
        AssetAmount(TokenId.fromStringUnsafe("03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04"), 2),
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

    val expectedRedeem = CFMMOrder.RedeemTokenFee(
      PoolId.fromStringUnsafe("9916d75132593c8b07fe18bd8d583bda1652eed7565cf41a4738ddd90fc992ec"),
      2000000,
      redeem.timestamp,
      RedeemParams(
        AssetAmount(
          TokenId.fromStringUnsafe("303f39026572bcb4060b51fafc93787a236bb243744babaa99fceb833d61e198"),
          11439
        ),
        dexFee = 15,
        redeemer =
          SErgoTree.unsafeFromString("0008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec")
      ),
      redeemV3
    )

    redeem shouldEqual expectedRedeem

  }

  property("Parse swap buy correct") {
    val swap = parser.swap(swapBuyV3).unsafeRunSync().get.asInstanceOf[SwapTokenFee]

    swap.params shouldEqual SwapParams(
      AssetAmount(
        TokenId.fromStringUnsafe("003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0"),
        22
      ),
      AssetAmount(
        TokenId.fromStringUnsafe("0000000000000000000000000000000000000000000000000000000000000000"),
        9852048
      ),
      1522526077827L,
      1000000000000000000L,
      SErgoTree.unsafeFromString("0008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec")
    )
    swap.poolId shouldEqual PoolId.fromStringUnsafe("1d5afc59838920bb5ef2a8f9d63825a55b1d48e269d7cecee335d637c3ff5f3f")
    swap.maxMinerFee shouldEqual 2000000
  }

  property("Parse swap sell correct") {
    val swap = parser.swap(swapSellV3).unsafeRunSync().get.asInstanceOf[SwapTokenFee]

    swap.params shouldEqual SwapParams(
      AssetAmount(
        TokenId.fromStringUnsafe("0000000000000000000000000000000000000000000000000000000000000000"),
        10000000
      ),
      AssetAmount(
        TokenId.fromStringUnsafe("003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0"),
        20
      ),
      75,
      100,
      SErgoTree.unsafeFromString("0008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec")
    )
    swap.poolId shouldEqual PoolId.fromStringUnsafe("1d5afc59838920bb5ef2a8f9d63825a55b1d48e269d7cecee335d637c3ff5f3f")
    swap.maxMinerFee shouldEqual 2000000

  }

  def swapSellV3 =
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

  def swapBuyV3 =
    io.circe.parser
      .decode[Output](
        s"""
           |{
           |    "boxId": "7134a244040ce51f1cdd6e8244414473cee6ed6f69efe3cd7829748b56770623",
           |    "transactionId": "0c4648c2152036a2ff15aa9094fdea094e8fb6c9031904b4d6e8b590a75abda8",
           |    "blockId": "4ea049c11f76c50a2b864472283743fa57ca677aef29981a8804493acca5e3ea",
           |    "value": 310000,
           |    "index": 0,
           |    "globalIndex": 26543227,
           |    "creationHeight": 940121,
           |    "settlementHeight": 940123,
           |    "ergoTree": "199c041a0400052c04c60f08cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec0404040604020524058080a0f6f4acdbe01b05868e82dacf5804000e201d5afc59838920bb5ef2a8f9d63825a55b1d48e269d7cecee335d637c3ff5f3f0e240008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec05a0d2b20906010004000e20003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d001010502040404d00f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730405000500058092f4010100d802d601b2a4730000d6029c73017e730205eb027303d195ed92b1a4730493b1db630872017305d804d603db63087201d604b2a5730600d60599c17204c1a7d606997e7307069d9c7e7205067e7308067e730906ededededed938cb27203730a0001730b93c27204730c927205730d95917206730ed801d607b2db63087204730f00ed938c7207017310927e8c7207020672067311909c7ec17201067e7202069c7e9a72057312069a9c7e8cb2720373130002067e7314067e72020690b0ada5d90107639593c272077315c1720773167317d90107599a8c7207018c72070273187319",
           |    "address": "4J8wkYtoeJ2gHH5jPzihSRcHcB5tgGFAEnL6kqJFeiAg3AcGcCbrqacccrU1rzJPfZLUyy7eBnbKUadAv7qnVboyYZFxKCbGTNg9N8ChGSTuk499nsA7ipKzgTxLbLwt9KjnJoXmqoFfBXFiiSWFWpw8rdnfpRoXZCGEbzmHvUWmwFMnMNrP6kGF769xooFTdWXnzA3YqKNdBr1wipvCFbYHzvW6pZukgRk8XNwby4Y51akY4EkSF85aWsTjhQThKrAXYCymGPAEDSbyrgyt8R3xpuBBXfvw2RXAFoj7tiXbetvMUx8wYc21aBdyTkZ3XMZc6rg4gRBS55uN9W3U85YjrKVQnSNR5MzuVinqBoHTh5CgC5AMCpVdH9LwWGoHzWMWacu5qrKJow95oG5Qs76sznCDdsmDJRtUCmAW7dGHevj33xXdHicdU4PxWTkenursT7Hk8NHwVwNMeUpnNqnty2RTZdys58U5HJ9ftsVhuuJ9PugUxSZTXQNbwEG7DhtK1oBynpvncp6fzkgf5BXjiXjr9FnDYsrbCZb6DtK9ydVFCSyWCkWBZrPdPsAjSadqeKKv2tgsy8g77NNTCwSuugosRWk3eCcnvPuEMiTE1S1wGS9F9Vib9isipMUngJkLNSGBMUGV1RkNVn6h3EKNqxAqtDpjPSd341vDsBsnbdqK3KnLszbJkSxJyw7Ktm5VFsbqtJcXf7wmchgMrq1MUYs7SiJwaGHmtcjr8o9b",
           |    "assets": [
           |        {
           |            "tokenId": "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
           |            "index": 0,
           |            "amount": 40,
           |            "name": "SigRSV",
           |            "decimals": 0,
           |            "type": "EIP-004"
           |        }
           |    ],
           |    "additionalRegisters": {},
           |    "spentTransactionId": "90c4848da58aa3b8ac6527fef8deb9844b66ea41ed42f85a3f5aa96493a0c61e",
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
           |    "boxId": "374f037640bf0dbf6e34244871937e75aaf110de9254ffcf09aa06291c11d375",
           |    "transactionId": "3da3aae612feee5ecf8d0af01e1205bdf25c01b09e5774517c8abd9387f81c03",
           |    "blockId": "258667b9c516bc0610017e92b3690c137d7aec673f6b6ad98d2c90fd2c31d780",
           |    "value": 12323164,
           |    "index": 0,
           |    "globalIndex": 26670124,
           |    "creationHeight": 942994,
           |    "settlementHeight": 942996,
           |    "ergoTree": "19c30417040005f8fbd80b08cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec040404060402040205feffffffffffffffff0104040504040004000e209916d75132593c8b07fe18bd8d583bda1652eed7565cf41a4738ddd90fc992ec0e240008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec0404040205c0b80201000e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730405000500058092f4010100d802d601b2a4730000d6027301eb027302d195ed92b1a4730393b1db630872017304d80bd603db63087201d604b2a5730500d605b27203730600d6067e9973078c72050206d6077ec1720106d6089d9c7e72020672067207d609b27203730800d60a7e8c72090206d60b9d9c7e7309067206720ad60cdb63087204d60db2720c730a00ededededed938cb27203730b0001730c93c27204730d95ed8f7208720b93b1720c730ed801d60eb2720c730f00eded92c1720499c1a77310938c720e018c720901927e8c720e02069d9c99720b7208720a720695927208720b927ec1720406997ec1a706997e7202069d9c997208720b720772067311938c720d018c720501927e8c720d0206a17208720b90b0ada5d9010e639593c2720e7312c1720e73137314d9010e599a8c720e018c720e0273157316",
           |    "address": "AVwRjyvectUWbmNGFBDonnfZue2PmZwhZtn3jBssdMdzBvnRw6moHaPGRAkesgkmfgs4cWU11Jxg1NQzNYzFE2VNwf4kVfxZfo7d2xD4YKEfP2oMMNnPyWVhTRt5vbCE4gEpbbcCDhY68dmx4Bx9bTd8eQQxJG2NzmSpuzU4qzVhrrqwg3Mq1sbc6zdADqzs2k8GQQt38GxG5QQLhJSnZEAB8TvEiMZGEKqPMATqY9HETxr13959bHLTyn2SvDdqDVKF7ZgpjEWmiqoikTaSLTGwfMopBXrE3bWTY26tFaL698zuPZ9zK6Ruz6kK3B2MCbFxemzxoiaoYnLfAjyjzczajp2ZP5eTasE76Ly2GtnAYnpfFa4VUAaSnCY2CQ2doSGwNCGd3DZE6e1btpewLUV33ZWAYj5NhxnrnXGPg1V9gz31HfbPiDMGdQcip41R99GmJLXfY3CoxLkJLPVYBXTvaRyNZHziE6JFHeMn41yTgKDBy9zoQLXsEn9v8vRJX9N44Ftc3wMR7hcfxgE2AdaH591ZFjEyV6FzKD6g53mGxfDNBaiqcLmvoE1Q26XyYzoac5weU9T39BbJaeETbZiJ81WH5CrcQmfZyYkRzypRKCD7cHnJ4MXQojo7Ff5gF5EP3Ta15eoFgcUDLKWCQTxirz5nBA3tqsZQuSD2A4FkQG1UZ3d27JJM7vjy5k1NJ6w3Ty4YaTUX2e2zm16sDN6L58oTSp21aeFcc952vX6FtbdYWUr68ifBux9ZCoS2sb7ZpacNz3NQvr8YnWY51QGPATZG4qagz",
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
           |            "amount": 15,
           |            "name": "SigRSV",
           |            "decimals": 0,
           |            "type": "EIP-004"
           |        }
           |    ],
           |    "additionalRegisters": {},
           |    "spentTransactionId": "e3023afd988ec2e10a4355edf346f7a6cfbd8d7ed9bd99c51702190b58163caa",
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
           |    "boxId": "29caadfbee140a978bd6200d807292160eb69b67a605fe597e887d8aae870faf",
           |    "transactionId": "dfdd1038cd12ad5615f0dc915de1f522359e45ab0fb1e54e6ee3f2a30ade7d24",
           |    "blockId": "7103bfc791be82d4b392292d23dc4b9808dd857ff728d019640c86490df41dc2",
           |    "value": 310000,
           |    "index": 0,
           |    "globalIndex": 26672272,
           |    "creationHeight": 943026,
           |    "settlementHeight": 943028,
           |    "ergoTree": "19ca0312040008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec04040406040204000404040005feffffffffffffffff01040204000e209916d75132593c8b07fe18bd8d583bda1652eed7565cf41a4738ddd90fc992ec0e240008cd03b196b978d77488fba3138876a40a40b9a046c2fbb5ecfa13d4ecf8f1eec52aec0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730405000500058092f4010100d801d601b2a4730000eb027301d195ed92b1a4730293b1db630872017303d806d602db63087201d603b2a5730400d604b2db63087203730500d605b27202730600d6067e8cb2db6308a77307000206d6077e9973088cb272027309000206ededededed938cb27202730a0001730b93c27203730c938c7204018c720501927e99c17203c1a7069d9c72067ec17201067207927e8c720402069d9c72067e8c72050206720790b0ada5d90108639593c27208730dc17208730e730fd90108599a8c7208018c72080273107311",
           |    "address": "4X2w8UZHL14TJyPYMRq5ABA4gUfetNKCQbgStifCP1zk8qmjxapNrQshQJ7ojuJwY3mRwBcxjYEBNNdnxPXkzExRnYi4bJJ86gD7eMEotAh2TnwGZKQ9wC8Gf3Faat6K2x8Lq4xJWZ1AYcJkXPDbEv5JTbtTRcudjfYkybdWMgbsB3Yz8dW3Yquju6L7t1y18qQZBjo4BcYuAsHUGmWpAvMYV31ywJsfuEMuxtMB8qE7PjW8mGqA1oQmxLc3wWC5yd17P7VArXGNoMdESKSs8EXA8wHBag2iR7Uy3wbQkTt3mXzi1KNm7PJgCw2QbGxvhgcwNQGtS8qJV4RkEabqKprfMozv2gECbv3371HrEhNVZHd4enejj3LphZfrTBSyNPWpAoGmxsLNVN7cakeCUApVhdebazfR8TAKXR6922QRojn7JGEuQauhARASTSqYsHTZZj11romGqRH5DY4YMZrcZvKJPB3fubyHwhCicirfYX745k2ZgjfsRCuQPiXrEaEziLRnpkbT8BDHoEGPX8to5kNH5TAhG1C2n24PeXhmUL9s3mrZ5XSvhY1WD6BXHXmHof6SouSnFiTMEWmtJ2FZeoizYVxS3McgPznyfioZCK1EYqZ7Tq3endLC",
           |    "assets": [
           |        {
           |            "tokenId": "303f39026572bcb4060b51fafc93787a236bb243744babaa99fceb833d61e198",
           |            "index": 0,
           |            "amount": 11439,
           |            "name": null,
           |            "decimals": null,
           |            "type": null
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
           |    "spentTransactionId": "bcd6c04442b8b5161d0a69c0ac6fe2ed20f812a135984824e6ca83e9b58aade1",
           |    "mainChain": true
           |}
           |""".stripMargin
      )
      .toOption
      .get

}
