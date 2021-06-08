package org.ergoplatform.dex.markets.modules

import org.ergoplatform.network.explorer.models.Items
import org.ergoplatform.network.models.Transaction

object samples {

  lazy val TradeTx: Transaction =
    io.circe.parser.decode[Items[Transaction]](TradeTxRaw).toOption.flatMap(_.items.headOption).get

  lazy val TradeTxRaw: String =
    """
      |{
      |    "items": [
      |        {
      |            "id": "7d1e76fd29e48025b672db8d3a9c4c141866bce0b4365770908649bc58893c14",
      |            "blockId": "74850a70b2c3cd4d2d8b8dc108f63166c578d31f34774e67d446d300afce7b2f",
      |            "inclusionHeight": 417978,
      |            "timestamp": 1612388208313,
      |            "index": 6,
      |            "numConfirmations": 20854,
      |            "inputs": [
      |                {
      |                    "boxId": "862be60b2cd53785e8efb0051b23acc7389581f5b897654555fae01bf9279a3b",
      |                    "value": 60000000,
      |                    "index": 0,
      |                    "spendingProof": null,
      |                    "outputBlockId": "e3f24e1bcec7a823528b97126069c956c9d27cad2d7bb32279d385986a2f8db2",
      |                    "outputTransactionId": "942f9c63a700479d1585db40f921eaaeba6aff1778a31c0d257a2f272ce84db8",
      |                    "outputIndex": 0,
      |                    "ergoTree": "102108cd03897bb50b78bf33d37fe29821d25acb1d7b909d1ff48626e2935762a2edf779450e207c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce0402040004020400040004000580b4891305000500010105000580b48913050005000580b4891304000402040005000580b489130580dac4090580b489130580dac409050004000580b4891304000500050204020400d805d6017300d602b5a5d9010263d801d604c67202040eedede6720493e47204c5a793c27202d07201d6037301d604b5a4d9010463d801d606c67204040eedede67206e6c672040505ed93e472067203d801d607db63087204ed93b172077302938cb27207730300017203d6058cc7a701eb027201d1ededed93b17202730491b172047305dad901060c63ed91b172067306d802d608d90108049272087205d609b272067307008cb07206860295da7208018cc7720901997308e5c6720905057309730a730bd9010a3c410163d804d60c8c720a02d60de5c6720c0505730cd60e95da7208018cc7720c0199730d720d730ed60f8c720a018602720eeded8c720f02ed91720d730f90720d731090720e8c720f0102017204d808d606b27202731100d607db63087206d6089593b1720773128cb27207731300027314d609b5a5d9010963d804d60bc67209040ed60cc672090505d60dc672090605d60ec67209070eededededede6720b93e4720b7203ede6720c93e4720c7315ede6720d93e4720d7316ede6720e93e4720ec5a793c27209c2a7d60ac1a7d60b9c72087317d60c9c73187208d60d8cb07204860272087319d9010d3c5963d805d60f8c720d02d6108c720d01d6118c721001d612a172118cb2db6308720f731a0002d6138c72100295dad90114049272147205018cc7720f01860299721172129a72139c99731be4c6720f050572128602997211721272130296830301938cb27207731c018602830002731d017203927208731eeceded93b17209731f93c1b272097320009999720a720b720c92c17206720ded9399720a720c720b92c17206720d",
      |                    "address": "221LzbM53YeSpWeiLT2XiGmewXkmpKZm37W8r3xVPpDXH5Qo8Vp6Kb7F6utkSteFuqhAFpQR6i3gY89aChXeisVm9PEM2bC2kHTkoccdvYQNveoiUjknzScVneDAdS1UKhY3KtUyWPLmsbacghnLsUcYEXDpv5ch5iHrCcofhmXaMr7Lqh3ZRyRLwwgrHibHLNbqz2fTADT7xQ3bDFjJ27rE1hc368HqdUjg4toEZWX4XqdY7x2juHVdR8rFnWk6aQjS3wFSFr3KJYG9WLSicgzxYYt2iKvFYpM7Vhhj3QCUNwzAz1n7qX7fGx8U3etgkDJyk4SMMzPed63jZJUcVRJMogxLzHg2SgdVkX8E97njyrFfinypwchCuU1wGzrdMmCL4bXLaU9agjbKZEvZVNEgGaTvcxiBKkeRPmmpQJV6Td7WnrSLyCw4hTMSm3b5pVkwKnQp3qKT4Pc5RM5Th9dfFJzUCrYcJA5zegUfBzwFhGyctPuyg5cp7eCRpdfuCXpxMLCW6nievyssntvyLdQu1N9f2uJAKv2c6kbQtQNTY2aRFn7bw8xJa3pLeuJVoB7DELTtXnngftSSfZ2pB2dfk3cFGktqxPtyLkZGnCveQ2R6sXdyvusfoH2Urg699ue4tgi3RtJE8kWpzkiWHYR2xg48fbe248uVJLu1LnYiJiPrsDdgEsLh2tq4218HBo86REFFs5DbHHwnJbX1YiAAT3kn7d1R5eDVXTuKRbTam3TnD7HGeNEoH7hepRprHUtBW5Yt5iBDzx9eXQ4hK7fRc1SeeJsS2cYMpzo5toSJP9eT8W8x4FYq9LnH6EF9A3Je3Zh1kjzuYQKoGeXtiHT8V3PYC2z9HmeWPT2GtJ4XpwPEHxXZva1aUeVzA5b6CnJQQtSCxgcDhsYDLHEC2pSi9v1UNKU6PSqFiezmYAFYF2Zfmz6EFGeWN7npUgt7cTA8LXtLR7GfqZAGsKQqdLX9ZLd5rrmeNDorpR3cSCk5NDAXjN1xw2TBcdAjfCBRscp9fR7cqWBsVA2or9bBbJ",
      |                    "assets": [],
      |                    "additionalRegisters": {
      |                        "R4": {
      |                            "serializedValue": "0e207c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce",
      |                            "sigmaType": "Coll[SByte]",
      |                            "renderedValue": "7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce"
      |                        },
      |                        "R5": {
      |                            "serializedValue": "0580b48913",
      |                            "sigmaType": "SLong",
      |                            "renderedValue": "20000000"
      |                        },
      |                        "R6": {
      |                            "serializedValue": "0580dac409",
      |                            "sigmaType": "SLong",
      |                            "renderedValue": "10000000"
      |                        }
      |                    }
      |                },
      |                {
      |                    "boxId": "59e5c427e919d2cf56a8c8efa5186c4e0a143de0d0e5298013cb6cf4164d5b82",
      |                    "value": 20000000,
      |                    "index": 1,
      |                    "spendingProof": null,
      |                    "outputBlockId": "7827b2d20c68c622824c36039646d9a5d4382e928afdc1fec70e584d0abbda83",
      |                    "outputTransactionId": "7b18ed164dead91413be9624acd0d11c3045963168230fa8ef1bb7b47400d800",
      |                    "outputIndex": 0,
      |                    "ergoTree": "101e08cd03e5c2313e2986dfd05984d1237cf815979cac01c61f29762d5b8d316a0ae1b9540e207c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce04000400040204000400040005000580b489130500010105000580b4891305000580b4891305000500040005000580b489130580b489130580ade2040580b4891304020400040005020580ade2040580b48913d806d6017300d602b5a5d9010263d801d604c67202040eedede6720493e47204c5a793c27202d07201d6037301d604b5a4d9010463d801d606c67204040eededede67206e6c672040505e6c672040605ed93e47206720393b1db630872047302d6058cc7a701d6068cb2db6308a773030002eb027201d1ededed93b17202730491b172047305dad901070c63ed91b172077306d802d609d90109049172097205d60ab272077307008cb07207860295da7209018cc7720a0199e5c6720a050573087309730a730bd9010b3c410163d804d60d8c720b02d60ee5c6720d0505730cd60f95da7209018cc7720d0199720e730d730ed6108c720b018602720fededed8c72100292720e730f91e5c6720d06057310731190720f8c72100102017204d803d607c1b27202731200d608d90108058cb07204860272087313d9010a3c5963d806d60c8c720a02d60d8c720a01d60e8c720d01d60fe4c6720c0505d610a1720e9dc1720c9a720fe4c6720c0605d6118c720d0295dad90112049172127205018cc7720c01860299720e72109a72119c99720f73147210860299720e7210721102d609b5a5d9010963d804d60bc67209040ed60cc672090505d60dc672090605d60ec67209070eededededede6720b93e4720b7203ede6720c93e4720c7315ede6720d93e4720d7316ede6720e93e4720ec5a793c27209c2a7ec9372079a9c72067317da7208017206ed93b172097318d803d60ab27209731900d60bb2db6308720a731a00d60c9972068c720b02ededed938c720b01720392720c731b93c1720a99c1a79c731c720c9372079a9c720c731dda720801720c",
      |                    "address": "75mZGYBd6r8LeR6TL2UomHx7kspa8Nubdi3VvaxoHrfiLMMiLzbLTYK52Gt2L2TKCFe4qM9XUws56kL4BZkkPuyxLzpuQvxRpkKaLbSmJM7jBHC44hKw7MkVVdcT9z37HTFxFMdp9jUoQZWMmRa9kMsCDDgCNu2u39GaxJebELtzRHu7QXWoQRFi4FZ7Sj2RRTwh6qAVbu9BudyYMx7mZ9AgmHqJyiwgxX7GviM8KHQkW7jWDE1AshhZYU7Pas1iasucHdM7L9iSgEEDtt39DpPi6wKBJYvmGmGvEekEbV82cZkYa55pw5r4pPb56YSQAkwDqUm7WVqECs9D2ZqzPFYkymkPDCv7UC7aC69bir2W38cmmeng3eWG9vvZBnWLtWsiqCkZGrBeygc3KuoP9JvHJVJ289poVw9edK2MzK2tP7Qks33qwTXjLDdBahZTAxvquSDmGprvEkvbTK5zJMapN8E8nMw6uWcRmWd7Ek6mLfFYMK89YqxRDPqFwtNJvpn1JhhKSg6xUFWz9sm7VJHYKZGmVq8coiZk6pKbeUpuQgPrLNAwnQ8ajPoov27aJYZpjTvnihYX13JVAEMi5pKQvotCpaAiPcpLe5KKnayvq9Nz4xMbcBZ4wxRzQe6S5m7AMEkkSerVXbFDFmBx2B6qzH8td86qDzhrTb3QiaveEgvaecRhVwLBxSVFuJsFSEaiHFGPb6iD6iEsxUWbmBBQYM6hgoGxcSwoaRe79UVCwKYLiKWSafay8QgjntrN4a6KwS9BX8TV1DZDPGL8jPc4TnDgawzEDXd1dJsnSyjz7od31TKtCYTphGqpYs373Cf7v3RbMWGDcm7nHPhc25NS6X6jH1RYBeEXGGT7PZLwh2qMxwJXg1uNRbpUgWWBSGKwf4eGyh6UYGjmiyyhRFVFzmxA1RzLTdWetpB4inCrkxm4gfM32Uq4kMmmFGqo1nSfh8avFGdQsmyEh1XWvZnBQBzLXatqRnqLPmpsc6X7xVHkP8PQG1f69ERbzxcS4FsXwoH27sip",
      |                    "assets": [
      |                        {
      |                            "tokenId": "7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce",
      |                            "index": 0,
      |                            "amount": 4,
      |                            "name": "EDEXTT",
      |                            "decimals": 0,
      |                            "type": "EIP-004"
      |                        }
      |                    ],
      |                    "additionalRegisters": {
      |                        "R4": {
      |                            "serializedValue": "0e207c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce",
      |                            "sigmaType": "Coll[SByte]",
      |                            "renderedValue": "7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce"
      |                        },
      |                        "R5": {
      |                            "serializedValue": "0580b48913",
      |                            "sigmaType": "SLong",
      |                            "renderedValue": "20000000"
      |                        },
      |                        "R6": {
      |                            "serializedValue": "0580ade204",
      |                            "sigmaType": "SLong",
      |                            "renderedValue": "5000000"
      |                        }
      |                    }
      |                }
      |            ],
      |            "outputs": [
      |                {
      |                    "boxId": "cc465ce958a034c8a7182af7135255f498a4cefe4624a12e98bf61a957898425",
      |                    "transactionId": "7d1e76fd29e48025b672db8d3a9c4c141866bce0b4365770908649bc58893c14",
      |                    "blockId": "74850a70b2c3cd4d2d8b8dc108f63166c578d31f34774e67d446d300afce7b2f",
      |                    "value": 28948160,
      |                    "index": 0,
      |                    "creationHeight": 417975,
      |                    "ergoTree": "0008cd02ddbe95b7f88d47bd8c2db823cc5dd1be69a650556a44d4c15ac65e1d3e34324c",
      |                    "address": "9gCigPc9cZNRhKgbgdmTkVxo1ZKgw79G8DvLjCcYWAvEF3XRUKy",
      |                    "assets": [],
      |                    "additionalRegisters": {},
      |                    "spentTransactionId": null,
      |                    "mainChain": true
      |                },
      |                {
      |                    "boxId": "14916d0a9b113f539c506f405561f23a8bcddc2d11f70ff8e7212f76c6085b1a",
      |                    "transactionId": "7d1e76fd29e48025b672db8d3a9c4c141866bce0b4365770908649bc58893c14",
      |                    "blockId": "74850a70b2c3cd4d2d8b8dc108f63166c578d31f34774e67d446d300afce7b2f",
      |                    "value": 1000000,
      |                    "index": 1,
      |                    "creationHeight": 417975,
      |                    "ergoTree": "1005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a57304",
      |                    "address": "2iHkR7CWvD1R4j1yZg5bkeDRQavjAaVPeTDFGGLZduHyfWMuYpmhHocX8GJoaieTx78FntzJbCBVL6rf96ocJoZdmWBL2fci7NqWgAirppPQmZ7fN9V6z13Ay6brPriBKYqLp1bT2Fk4FkFLCfdPpe",
      |                    "assets": [],
      |                    "additionalRegisters": {},
      |                    "spentTransactionId": "2d9c712c8c04474a09a8c5e7469c22da9e0b06c6cdeeefa635f535bc4ab75c2e",
      |                    "mainChain": true
      |                },
      |                {
      |                    "boxId": "75ee594578fca2923721791f91e1188d337051e24b6893cbdbc1a11e6416748f",
      |                    "transactionId": "7d1e76fd29e48025b672db8d3a9c4c141866bce0b4365770908649bc58893c14",
      |                    "blockId": "74850a70b2c3cd4d2d8b8dc108f63166c578d31f34774e67d446d300afce7b2f",
      |                    "value": 10000000,
      |                    "index": 2,
      |                    "creationHeight": 417975,
      |                    "ergoTree": "101e08cd03e5c2313e2986dfd05984d1237cf815979cac01c61f29762d5b8d316a0ae1b9540e207c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce04000400040204000400040005000580b489130500010105000580b4891305000580b4891305000500040005000580b489130580b489130580ade2040580b4891304020400040005020580ade2040580b48913d806d6017300d602b5a5d9010263d801d604c67202040eedede6720493e47204c5a793c27202d07201d6037301d604b5a4d9010463d801d606c67204040eededede67206e6c672040505e6c672040605ed93e47206720393b1db630872047302d6058cc7a701d6068cb2db6308a773030002eb027201d1ededed93b17202730491b172047305dad901070c63ed91b172077306d802d609d90109049172097205d60ab272077307008cb07207860295da7209018cc7720a0199e5c6720a050573087309730a730bd9010b3c410163d804d60d8c720b02d60ee5c6720d0505730cd60f95da7209018cc7720d0199720e730d730ed6108c720b018602720fededed8c72100292720e730f91e5c6720d06057310731190720f8c72100102017204d803d607c1b27202731200d608d90108058cb07204860272087313d9010a3c5963d806d60c8c720a02d60d8c720a01d60e8c720d01d60fe4c6720c0505d610a1720e9dc1720c9a720fe4c6720c0605d6118c720d0295dad90112049172127205018cc7720c01860299720e72109a72119c99720f73147210860299720e7210721102d609b5a5d9010963d804d60bc67209040ed60cc672090505d60dc672090605d60ec67209070eededededede6720b93e4720b7203ede6720c93e4720c7315ede6720d93e4720d7316ede6720e93e4720ec5a793c27209c2a7ec9372079a9c72067317da7208017206ed93b172097318d803d60ab27209731900d60bb2db6308720a731a00d60c9972068c720b02ededed938c720b01720392720c731b93c1720a99c1a79c731c720c9372079a9c720c731dda720801720c",
      |                    "address": "75mZGYBd6r8LeR6TL2UomHx7kspa8Nubdi3VvaxoHrfiLMMiLzbLTYK52Gt2L2TKCFe4qM9XUws56kL4BZkkPuyxLzpuQvxRpkKaLbSmJM7jBHC44hKw7MkVVdcT9z37HTFxFMdp9jUoQZWMmRa9kMsCDDgCNu2u39GaxJebELtzRHu7QXWoQRFi4FZ7Sj2RRTwh6qAVbu9BudyYMx7mZ9AgmHqJyiwgxX7GviM8KHQkW7jWDE1AshhZYU7Pas1iasucHdM7L9iSgEEDtt39DpPi6wKBJYvmGmGvEekEbV82cZkYa55pw5r4pPb56YSQAkwDqUm7WVqECs9D2ZqzPFYkymkPDCv7UC7aC69bir2W38cmmeng3eWG9vvZBnWLtWsiqCkZGrBeygc3KuoP9JvHJVJ289poVw9edK2MzK2tP7Qks33qwTXjLDdBahZTAxvquSDmGprvEkvbTK5zJMapN8E8nMw6uWcRmWd7Ek6mLfFYMK89YqxRDPqFwtNJvpn1JhhKSg6xUFWz9sm7VJHYKZGmVq8coiZk6pKbeUpuQgPrLNAwnQ8ajPoov27aJYZpjTvnihYX13JVAEMi5pKQvotCpaAiPcpLe5KKnayvq9Nz4xMbcBZ4wxRzQe6S5m7AMEkkSerVXbFDFmBx2B6qzH8td86qDzhrTb3QiaveEgvaecRhVwLBxSVFuJsFSEaiHFGPb6iD6iEsxUWbmBBQYM6hgoGxcSwoaRe79UVCwKYLiKWSafay8QgjntrN4a6KwS9BX8TV1DZDPGL8jPc4TnDgawzEDXd1dJsnSyjz7od31TKtCYTphGqpYs373Cf7v3RbMWGDcm7nHPhc25NS6X6jH1RYBeEXGGT7PZLwh2qMxwJXg1uNRbpUgWWBSGKwf4eGyh6UYGjmiyyhRFVFzmxA1RzLTdWetpB4inCrkxm4gfM32Uq4kMmmFGqo1nSfh8avFGdQsmyEh1XWvZnBQBzLXatqRnqLPmpsc6X7xVHkP8PQG1f69ERbzxcS4FsXwoH27sip",
      |                    "assets": [
      |                        {
      |                            "tokenId": "7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce",
      |                            "index": 0,
      |                            "amount": 2,
      |                            "name": "EDEXTT",
      |                            "decimals": 0,
      |                            "type": "EIP-004"
      |                        }
      |                    ],
      |                    "additionalRegisters": {
      |                        "R4": {
      |                            "serializedValue": "0e207c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce",
      |                            "sigmaType": "Coll[SByte]",
      |                            "renderedValue": "7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce"
      |                        },
      |                        "R5": {
      |                            "serializedValue": "0580b48913",
      |                            "sigmaType": "SLong",
      |                            "renderedValue": "20000000"
      |                        },
      |                        "R6": {
      |                            "serializedValue": "0580ade204",
      |                            "sigmaType": "SLong",
      |                            "renderedValue": "5000000"
      |                        },
      |                        "R7": {
      |                            "serializedValue": "0e2059e5c427e919d2cf56a8c8efa5186c4e0a143de0d0e5298013cb6cf4164d5b82",
      |                            "sigmaType": "Coll[SByte]",
      |                            "renderedValue": "59e5c427e919d2cf56a8c8efa5186c4e0a143de0d0e5298013cb6cf4164d5b82"
      |                        }
      |                    },
      |                    "spentTransactionId": null,
      |                    "mainChain": true
      |                },
      |                {
      |                    "boxId": "00a26a6141e5a2f2308efcadc0bba5dcf6f16fd4e49990b6bee0cc6b4c41f1e7",
      |                    "transactionId": "7d1e76fd29e48025b672db8d3a9c4c141866bce0b4365770908649bc58893c14",
      |                    "blockId": "74850a70b2c3cd4d2d8b8dc108f63166c578d31f34774e67d446d300afce7b2f",
      |                    "value": 40000000,
      |                    "index": 3,
      |                    "creationHeight": 417975,
      |                    "ergoTree": "0008cd03e5c2313e2986dfd05984d1237cf815979cac01c61f29762d5b8d316a0ae1b954",
      |                    "address": "9iCzZksco8R2P8HXTsZiFAq2km59PDznuTykBRjHd74BfBG3kk8",
      |                    "assets": [],
      |                    "additionalRegisters": {
      |                        "R4": {
      |                            "serializedValue": "0e2059e5c427e919d2cf56a8c8efa5186c4e0a143de0d0e5298013cb6cf4164d5b82",
      |                            "sigmaType": "Coll[SByte]",
      |                            "renderedValue": "59e5c427e919d2cf56a8c8efa5186c4e0a143de0d0e5298013cb6cf4164d5b82"
      |                        }
      |                    },
      |                    "spentTransactionId": null,
      |                    "mainChain": true
      |                },
      |                {
      |                    "boxId": "986734545f366be43531bedf3fd3965f13d928311bbb7133a9f5553cfb875bbc",
      |                    "transactionId": "7d1e76fd29e48025b672db8d3a9c4c141866bce0b4365770908649bc58893c14",
      |                    "blockId": "74850a70b2c3cd4d2d8b8dc108f63166c578d31f34774e67d446d300afce7b2f",
      |                    "value": 51840,
      |                    "index": 4,
      |                    "creationHeight": 417975,
      |                    "ergoTree": "0008cd03897bb50b78bf33d37fe29821d25acb1d7b909d1ff48626e2935762a2edf77945",
      |                    "address": "9hWMWtGho2VBPsSRigWMUUtk9sWWPFKSdDWcxSvV9TiTB4PCRKc",
      |                    "assets": [
      |                        {
      |                            "tokenId": "7c232b68665d233356e9abadf3820abff725105c5ccfa8618b77bc3a8bf603ce",
      |                            "index": 0,
      |                            "amount": 2,
      |                            "name": "EDEXTT",
      |                            "decimals": 0,
      |                            "type": "EIP-004"
      |                        }
      |                    ],
      |                    "additionalRegisters": {
      |                        "R4": {
      |                            "serializedValue": "0e20862be60b2cd53785e8efb0051b23acc7389581f5b897654555fae01bf9279a3b",
      |                            "sigmaType": "Coll[SByte]",
      |                            "renderedValue": "862be60b2cd53785e8efb0051b23acc7389581f5b897654555fae01bf9279a3b"
      |                        }
      |                    },
      |                    "spentTransactionId": null,
      |                    "mainChain": true
      |                }
      |            ]
      |        }
      |    ],
      |    "total": 1
      |}
      |""".stripMargin
}
