package org.ergoplatform.dex.tracker.parsers.amm

import io.circe.parser.decode
import org.ergoplatform.ergo.services.explorer.models.Output

object V3Orders {

  val deployV3SwapSell =
    decode[Output](
      """
        |{
        |    "boxId": "f027c8ec3632686b6f13a3f9bc7841114db759d0901c3efe5976ea6d3729c905",
        |    "transactionId": "4b9a9ab331777a51a2f1d706ffa47688ff0c48441385a0005ba382071d0514b3",
        |    "blockId": "f23b065123a51b94a4f9e1dcfa017c8561aecd63682e140d68d0bd54938b5d67",
        |    "value": 300400000,
        |    "index": 0,
        |    "globalIndex": 27082285,
        |    "creationHeight": 952305,
        |    "settlementHeight": 952307,
        |    "ergoTree": "199505210400058080d0d88bdea2e30205a2d4e0f5a18c87da0205808c8d9e0204ca0f08cd034153f4c52b1d3e301236cfc939f27dc6c58822c160387865ee1e41d407827cb40404040604020400010105e2f81304000e20f40afb6f877c40a30c8637dd5362227285738174151ce66d6684bc1b727ab6cf0e240008cd034153f4c52b1d3e301236cfc939f27dc6c58822c160387865ee1e41d407827cb40e209a06d9e545a41fd51eeffc5e20d818073bf820c635e2a9d922269913e0de369d05a0d780050101010105f015060100040404020e209a06d9e545a41fd51eeffc5e20d818073bf820c635e2a9d922269913e0de369d0101040406010104d00f0e691005040004000e36100204a00b08cd0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798ea02d192a39a8cc7a701730073011001020402d19683030193a38cc7b2a57300000193c2b2a57301007473027303830108cdeeac93b1a5730405000500058092f4010100d804d601b2a4730000d6027301d6037302d6049c73037e730405eb027305d195ed92b1a4730693b1db630872017307d806d605db63087201d606b2a5730800d607db63087206d608b27207730900d6098c720802d60a95730a9d9c7e997209730b067e7202067e7203067e720906edededededed938cb27205730c0001730d93c27206730e938c720801730f92720a7e7310069573117312d801d60b997e7313069d9c720a7e7203067e72020695ed91720b731492b172077315d801d60cb27207731600ed938c720c017317927e8c720c0206720b7318909c7e8cb2720573190002067e7204069c9a720a731a9a9c7ec17201067e731b067e72040690b0ada5d9010b639593c2720b731cc1720b731d731ed9010b599a8c720b018c720b02731f7320",
        |    "address": "9u7gKQspeuiD4PLaxV56gsMncqj7VxhF19n8t9HzQE1n8w4oCbgfYEQvqfpuDQhVGAinQtgpyym5SsBt8aqynZE2uDeBcbKpovxfRuzirXcDWxk97BXCYXgcyqZb9wxphG5L3ZyWbWTRFGvh8XU9N3zVHsk6gYAuwctus6KRGQCMueSigGLWv6S94nMAKu2iiThMQQ5Er4BaG4ePXP2P3t1fngRfFupqBcw77tLDUipD9wsBsWcsaf766356ikZ1jaPZkX4aJPnVJsHNhxRWgb7yZqG8NJEdepXyvh2yqFmRXo4gmugYryAE9UfpKmaKJ6E2BZfyUFn4tyEemPUb7FDrp1s1kSYpa2v2BcYbGkQeiRVPa5ipEJ2bpfAtjaTVQkstoLqNL4SwepaVGHfjDC5X2bJ3YdNAxSiKuPPikEkuHoY5G3WZGcsZRceQ5AYgmi34pAPzHqHTRKfRMvwcPfrEdmsuBCHi9dvANxHShMLNEV6cULYBjRx4b78TocwNb6LHo87YgFm7C4jXM1NAQT4Dt74SfrmyA8AW3QyvV4a7T2uHhrkRucMXHkW4UiQdRT1iV99WEp5AP8zwvfaLo13JE7sNYR49XQVHd21p1WsV3Am8GrSAMmA9LUMLmkvgKngh2TbyEu14rrdmGCpBWnWYq1XVQiBWHferyzaDjscLkdhckNZKzSjBpF9rDRZLRTjcqFwsJxmevNkhi2MwTjXernXVJVY1Bakr1LrnycRdH7HyV35nBauoqJrC3jLuJgE8VMAF2cAtdSSkYGXDSEPLZpLkyN5KnbHtNcMmKz6PBZodyWeVkpbGjMce7uv6LdWhGABSDbvtZj5ty3FY3K4QGGfBcTz4c7mod6g8bP2fVqLXDJkbB2mMPUFxXHZqjc1FudHRTuo1ZREw4",
        |    "assets": [
        |        {
        |            "tokenId": "9a06d9e545a41fd51eeffc5e20d818073bf820c635e2a9d922269913e0de369d",
        |            "index": 0,
        |            "amount": 163377,
        |            "name": "SPF",
        |            "decimals": 6,
        |            "type": "EIP-004"
        |        }
        |    ],
        |    "additionalRegisters": {},
        |    "spentTransactionId": null,
        |    "mainChain": true
        |}
        |""".stripMargin
    ).toOption.get

}
