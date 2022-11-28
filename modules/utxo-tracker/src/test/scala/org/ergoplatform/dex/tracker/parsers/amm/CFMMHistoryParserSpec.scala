package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.IO
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.tracker.parsers.amm.analytics.CFMMHistoryParser
import org.ergoplatform.ergo.domain.SettledTransaction
import org.ergoplatform.ergo.services.explorer.models.{Transaction => ExplorerTX}
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class CFMMHistoryParserSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with CatsPlatform {

  implicit val e: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  property("AMM Swap parsing") {
    val p      = CFMMHistoryParser.t2tCFMMHistory[IO]
    val parseF = p.swap(SettledTransaction.fromExplorer(txSample))
    println(parseF.unsafeRunSync())
  }

  val txSample =
    io.circe.parser
      .decode[ExplorerTX]("""
      |{
      |    "id": "adcf4903c81b3bfaae98c286fb4a2593f1119c61ce2120c6b664fc297a75ab9a",
      |    "blockId": "0245ec3acebdcaf9be3435498cee4870ef8f5fbc9d662857772adcf3525c6a74",
      |    "inclusionHeight": 634959,
      |    "timestamp": 1638713207827,
      |    "index": 16,
      |    "globalIndex": 2241373,
      |    "numConfirmations": 800,
      |    "inputs": [
      |        {
      |            "boxId": "506ea136302aa4f7ee1b1ce0d851f09b611932e6a9f18f5e0ad1508bdf5c236e",
      |            "value": 4000000,
      |            "index": 0,
      |            "spendingProof": null,
      |            "outputBlockId": "b88132dc968d65d63fa6af0af1743a330cb6dcb7d5a0f611950629c61ed11fa0",
      |            "outputTransactionId": "9328e103ab17318cbe85539a9c85ec0e594374f0ca88bb16a613943a698e9584",
      |            "outputIndex": 0,
      |            "outputGlobalIndex": 10502158,
      |            "outputCreatedAt": 634953,
      |            "outputSettledAt": 634955,
      |            "ergoTree": "19a9030f040004020402040404040406040605feffffffffffffffff0105feffffffffffffffff01050004d00f0400040005000500d81ad601b2a5730000d602e4c6a70404d603db63087201d604db6308a7d605b27203730100d606b27204730200d607b27203730300d608b27204730400d609b27203730500d60ab27204730600d60b9973078c720602d60c999973088c720502720bd60d8c720802d60e998c720702720dd60f91720e7309d6108c720a02d6117e721006d6127e720e06d613998c7209027210d6147e720d06d615730ad6167e721306d6177e720c06d6187e720b06d6199c72127218d61a9c72167218d1edededededed93c27201c2a793e4c672010404720292c17201c1a793b27203730b00b27204730c00938c7205018c720601ed938c7207018c720801938c7209018c720a019593720c730d95720f929c9c721172127e7202069c7ef07213069a9c72147e7215067e9c720e7e72020506929c9c721472167e7202069c7ef0720e069a9c72117e7215067e9c72137e7202050695ed720f917213730e907217a19d721972149d721a7211ed9272199c7217721492721a9c72177211",
      |            "address": "3gb1RZucekcRdda82TSNS4FZSREhGLoi1FxGDmMZdVeLtYYixPRviEdYireoM9RqC6Jf4kx85Y1jmUg5XzGgqdjpkhHm7kJZdgUR3VBwuLZuyHVqdSNv3eanqpknYsXtUwvUA16HFwNa3HgVRAnGC8zj8U7kksrfjycAM1yb19BB4TYR2BKWN7mpvoeoTuAKcAFH26cM46CEYsDRDn832wVNTLAmzz4Q6FqE29H9euwYzKiebgxQbWUxtupvfSbKaHpQcZAo5Dhyc6PFPyGVFZVRGZZ4Kftgi1NMRnGwKG7NTtXsFMsJP6A7yvLy8UZaMPe69BUAkpbSJdcWem3WpPUE7UpXv4itDkS5KVVaFtVyfx8PQxzi2eotP2uXtfairHuKinbpSFTSFKW3GxmXaw7vQs1JuVd8NhNShX6hxSqCP6sxojrqBxA48T2KcxNrmE3uFk7Pt4vPPdMAS4PW6UU82UD9rfhe3SMytK6DkjCocuRwuNqFoy4k25TXbGauTNgKuPKY3CxgkTpw9WfWsmtei178tLefhUEGJueueXSZo7negPYtmcYpoMhCuv4G1JZc283Q7f3mNXS",
      |            "assets": [
      |                {
      |                    "tokenId": "080e453271ff4d2f85f97569b09755e67537bf2a1bc1cd09411b459cf901b902",
      |                    "index": 0,
      |                    "amount": 1,
      |                    "name": null,
      |                    "decimals": null,
      |                    "type": null
      |                },
      |                {
      |                    "tokenId": "ef8b8067973e3f96e3b39d54a9b53039414986392b1861ed572db67ac96f7f60",
      |                    "index": 1,
      |                    "amount": 9223372036854726341,
      |                    "name": "SigUSD_SigRSV_LP",
      |                    "decimals": 0,
      |                    "type": "EIP-004"
      |                },
      |                {
      |                    "tokenId": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
      |                    "index": 2,
      |                    "amount": 52406,
      |                    "name": "SigUSD",
      |                    "decimals": 2,
      |                    "type": "EIP-004"
      |                },
      |                {
      |                    "tokenId": "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
      |                    "index": 3,
      |                    "amount": 40146,
      |                    "name": "SigRSV",
      |                    "decimals": 0,
      |                    "type": "EIP-004"
      |                }
      |            ],
      |            "additionalRegisters": {
      |                "R4": {
      |                    "serializedValue": "04c60f",
      |                    "sigmaType": "SInt",
      |                    "renderedValue": "995"
      |                }
      |            }
      |        },
      |        {
      |            "boxId": "8ccb7b2a2e8852defeea6ea65cacd2f296aa9f54e10830df8ac002d17c428435",
      |            "value": 8460000,
      |            "index": 1,
      |            "spendingProof": null,
      |            "outputBlockId": "0245ec3acebdcaf9be3435498cee4870ef8f5fbc9d662857772adcf3525c6a74",
      |            "outputTransactionId": "bafd9e955891d6c62fb6e3b9fff0ae65c9fda01ed6d6164f93f551cc380f62d0",
      |            "outputIndex": 0,
      |            "outputGlobalIndex": 10502312,
      |            "outputCreatedAt": 508928,
      |            "outputSettledAt": 634959,
      |            "ergoTree": "19ad041708cd02c3f56e66191a903758f53a4b90d07cef80f93e7a4f17d106098ad0caf189722a04000e20003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d004c60f04d00f040404080402040004040400040606010104000e20080e453271ff4d2f85f97569b09755e67537bf2a1bc1cd09411b459cf901b90205806405feac0d05c8010e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730405000500058092f4010100d805d6017300d602b2a4730100d6037302d6047303d6057304eb027201d195ed93b1a4730593b1db630872027306d80ad606db63087202d607b2a5730700d608b2db63087207730800d6098c720802d60a7e720906d60bb27206730900d60c7e8c720b0206d60d7e8cb2db6308a7730a000206d60e7e8cb27206730b000206d60f9a720a730cedededededed938cb27206730d0001730e93c27207d07201938c7208017203927209730f927ec1720706997ec1a7069d9c720a7e7310067e73110695938c720b017203909c9c720c720d7e7204069c720f9a9c720e7e7205069c720d7e720406909c9c720e720d7e7204069c720f9a9c720c7e7205069c720d7e72040690b0ada5d90110639593c272107312c1721073137314d90110599a8c7210018c72100273157316",
      |            "address": "8vrKoDbxwy3ChnzbDEmmAtFLb1SoyArcbQP6UMy8gMBcJ4Q1bHVLg5pj4Lp9D1igQtAVVxNCe6gtQbhEg3d5LSBJS6T92nYzCYmaS8StU5SA5dBd1mUMuj6WUDiSY39NikdKNkZUztXjRdmYVtMPhFHfZwBhKzTuUjmn8BLfWGdHYJq5Rf8RkCSRorcty3YLkhoNFULf2qEC1PXMg2uWzZcfa3MjW45SMCAgca3S6MUWUK8ofwMeLawATschLX8faBEaFqus8Xf3gyz6Dsu8MVzrqCBSFqPwAHHGtfPwGvzMCLDtYNzY3SpJuwMixG6skja21ip9H61kZkV6N1t8Z76EQ8KgL8ETDaRYhc1VAHkLh6RHGvQsK3xQQSteYLEAwh4KipV3PVjoQ24TwRGZrd1cpGgovEBfV5JFJ9SMQebTDfKEq5xjuPoqwgRGxCnEj58hAKSXDtb66dfAQwp4vA4EFGYHvgp4DGeQQjizGf2MvtCTF2hvXFBEMf2U5Y2fVKAopDpetMjJceB4S4KnudGmJMECxgMjbvZ6Smg4b6B3PwhdzwLkWFA5T4cqKLdCyaPA21SQFxketojgJ2TtTiihVXrtiu9ZtyVjA8WX2cPbQVYgZB9aj81zwdF7WnCxi3exT16mUzdKgp8ABg7mRhKE6s7y9T7ANs8tRbWLunciNs5fRfdqtWtLJCZvjUEw19wtuGZijoJDZEBFByG63Cvd9CsWrjfkzVkt2Cok96TQEKksev1QQCyzZJixwRQLeJD",
      |            "assets": [
      |                {
      |                    "tokenId": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
      |                    "index": 0,
      |                    "amount": 10000,
      |                    "name": "SigUSD",
      |                    "decimals": 2,
      |                    "type": "EIP-004"
      |                }
      |            ],
      |            "additionalRegisters": {}
      |        }
      |    ],
      |    "dataInputs": [],
      |    "outputs": [
      |        {
      |            "boxId": "fec5bc08f17a54c2477cfadb77ae34d8ac05ed5a7fe9cdcbc0ab5abf80222002",
      |            "transactionId": "adcf4903c81b3bfaae98c286fb4a2593f1119c61ce2120c6b664fc297a75ab9a",
      |            "blockId": "0245ec3acebdcaf9be3435498cee4870ef8f5fbc9d662857772adcf3525c6a74",
      |            "value": 4000000,
      |            "index": 0,
      |            "globalIndex": 10502320,
      |            "creationHeight": 634957,
      |            "settlementHeight": 634959,
      |            "ergoTree": "19a9030f040004020402040404040406040605feffffffffffffffff0105feffffffffffffffff01050004d00f0400040005000500d81ad601b2a5730000d602e4c6a70404d603db63087201d604db6308a7d605b27203730100d606b27204730200d607b27203730300d608b27204730400d609b27203730500d60ab27204730600d60b9973078c720602d60c999973088c720502720bd60d8c720802d60e998c720702720dd60f91720e7309d6108c720a02d6117e721006d6127e720e06d613998c7209027210d6147e720d06d615730ad6167e721306d6177e720c06d6187e720b06d6199c72127218d61a9c72167218d1edededededed93c27201c2a793e4c672010404720292c17201c1a793b27203730b00b27204730c00938c7205018c720601ed938c7207018c720801938c7209018c720a019593720c730d95720f929c9c721172127e7202069c7ef07213069a9c72147e7215067e9c720e7e72020506929c9c721472167e7202069c7ef0720e069a9c72117e7215067e9c72137e7202050695ed720f917213730e907217a19d721972149d721a7211ed9272199c7217721492721a9c72177211",
      |            "address": "3gb1RZucekcRdda82TSNS4FZSREhGLoi1FxGDmMZdVeLtYYixPRviEdYireoM9RqC6Jf4kx85Y1jmUg5XzGgqdjpkhHm7kJZdgUR3VBwuLZuyHVqdSNv3eanqpknYsXtUwvUA16HFwNa3HgVRAnGC8zj8U7kksrfjycAM1yb19BB4TYR2BKWN7mpvoeoTuAKcAFH26cM46CEYsDRDn832wVNTLAmzz4Q6FqE29H9euwYzKiebgxQbWUxtupvfSbKaHpQcZAo5Dhyc6PFPyGVFZVRGZZ4Kftgi1NMRnGwKG7NTtXsFMsJP6A7yvLy8UZaMPe69BUAkpbSJdcWem3WpPUE7UpXv4itDkS5KVVaFtVyfx8PQxzi2eotP2uXtfairHuKinbpSFTSFKW3GxmXaw7vQs1JuVd8NhNShX6hxSqCP6sxojrqBxA48T2KcxNrmE3uFk7Pt4vPPdMAS4PW6UU82UD9rfhe3SMytK6DkjCocuRwuNqFoy4k25TXbGauTNgKuPKY3CxgkTpw9WfWsmtei178tLefhUEGJueueXSZo7negPYtmcYpoMhCuv4G1JZc283Q7f3mNXS",
      |            "assets": [
      |                {
      |                    "tokenId": "080e453271ff4d2f85f97569b09755e67537bf2a1bc1cd09411b459cf901b902",
      |                    "index": 0,
      |                    "amount": 1,
      |                    "name": null,
      |                    "decimals": null,
      |                    "type": null
      |                },
      |                {
      |                    "tokenId": "ef8b8067973e3f96e3b39d54a9b53039414986392b1861ed572db67ac96f7f60",
      |                    "index": 1,
      |                    "amount": 9223372036854726341,
      |                    "name": "SigUSD_SigRSV_LP",
      |                    "decimals": 0,
      |                    "type": "EIP-004"
      |                },
      |                {
      |                    "tokenId": "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
      |                    "index": 2,
      |                    "amount": 62406,
      |                    "name": "SigUSD",
      |                    "decimals": 2,
      |                    "type": "EIP-004"
      |                },
      |                {
      |                    "tokenId": "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
      |                    "index": 3,
      |                    "amount": 33740,
      |                    "name": "SigRSV",
      |                    "decimals": 0,
      |                    "type": "EIP-004"
      |                }
      |            ],
      |            "additionalRegisters": {
      |                "R4": {
      |                    "serializedValue": "04c60f",
      |                    "sigmaType": "SInt",
      |                    "renderedValue": "995"
      |                }
      |            },
      |            "spentTransactionId": "82a55f397ac5c8a8aecb25572e5c7651f97fe7a956b0f8c5f8d21c04722baef6",
      |            "mainChain": true
      |        },
      |        {
      |            "boxId": "82f1be4cdc795c74ba73b92e123c8831d6c34aa67ad33eafba67636a3b24d3a5",
      |            "transactionId": "adcf4903c81b3bfaae98c286fb4a2593f1119c61ce2120c6b664fc297a75ab9a",
      |            "blockId": "0245ec3acebdcaf9be3435498cee4870ef8f5fbc9d662857772adcf3525c6a74",
      |            "value": 1453438,
      |            "index": 1,
      |            "globalIndex": 10502321,
      |            "creationHeight": 634957,
      |            "settlementHeight": 634959,
      |            "ergoTree": "0008cd02c3f56e66191a903758f53a4b90d07cef80f93e7a4f17d106098ad0caf189722a",
      |            "address": "9g1N1xqhrNG1b2TkmFcQGTFZ47EquUYUZAiWWCBEbZaBcsMhXJU",
      |            "assets": [
      |                {
      |                    "tokenId": "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
      |                    "index": 0,
      |                    "amount": 6406,
      |                    "name": "SigRSV",
      |                    "decimals": 0,
      |                    "type": "EIP-004"
      |                }
      |            ],
      |            "additionalRegisters": {},
      |            "spentTransactionId": null,
      |            "mainChain": true
      |        },
      |        {
      |            "boxId": "e1605408b042bc7ad39d13b08b216208d20215363fbe6640647b24c3d21b865b",
      |            "transactionId": "adcf4903c81b3bfaae98c286fb4a2593f1119c61ce2120c6b664fc297a75ab9a",
      |            "blockId": "0245ec3acebdcaf9be3435498cee4870ef8f5fbc9d662857772adcf3525c6a74",
      |            "value": 5006562,
      |            "index": 2,
      |            "globalIndex": 10502322,
      |            "creationHeight": 634957,
      |            "settlementHeight": 634959,
      |            "ergoTree": "0008cd03412b7ad71f183e6045fc184d028d02ac6a66a48021868ff9c6470d31fc56ea07",
      |            "address": "9gxWNGuK6ga1QEkja5Dj39s8HC47BfKkDf6fmzoAW4AStYPne7A",
      |            "assets": [],
      |            "additionalRegisters": {},
      |            "spentTransactionId": null,
      |            "mainChain": true
      |        },
      |        {
      |            "boxId": "b188679fc930f0cde59a355c5a0d21075f2b23cc5076cbfebc23c407002fe978",
      |            "transactionId": "adcf4903c81b3bfaae98c286fb4a2593f1119c61ce2120c6b664fc297a75ab9a",
      |            "blockId": "0245ec3acebdcaf9be3435498cee4870ef8f5fbc9d662857772adcf3525c6a74",
      |            "value": 2000000,
      |            "index": 3,
      |            "globalIndex": 10502323,
      |            "creationHeight": 634957,
      |            "settlementHeight": 634959,
      |            "ergoTree": "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304",
      |            "address": "2iHkR7CWvD1R4j1yZg5bkeDRQavjAaVPeTDFGGLZduHyfWMuYpmhHocX8GJoaieTx78FntzJbCBVL6rf96ocJoZdmWBL2fci7NqWgAirppPQmZ7fN9V6z13Ay6brPriBKYqLp1bT2Fk4FkFLCfdPpe",
      |            "assets": [],
      |            "additionalRegisters": {},
      |            "spentTransactionId": "5f35f0cc5032a903398e2610595f02d5071a86709c2eab5eb9e5296fb9dad106",
      |            "mainChain": true
      |        }
      |    ],
      |    "size": 865
      |}
      |""".stripMargin)
      .toOption
      .get
}
