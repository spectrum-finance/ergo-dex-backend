package org.ergoplatform.dex.tracker.parsers.amm

import cats.effect.{Clock, SyncIO}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.configs.MonetaryConfig
import org.ergoplatform.dex.protocol.amm.{AMMType, ParserVersion}
import org.ergoplatform.dex.tracker.parsers.amm.v3.N2TOrdersV3Parser
import org.ergoplatform.dex.tracker.validation.amm.CfmmRuleDefs
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.ergo.domain.Output
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class N2TV3ParserSpec2 extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with CatsPlatform {

  implicit val clock: Clock[SyncIO] = Clock.create

  implicit val e: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)

  val spf = TokenId.fromStringUnsafe("003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0")

  property("Validate v3 n2t swap correct") {
    def parser: CFMMOrdersParser[AMMType.N2T_CFMM, ParserVersion.V3, SyncIO] =
      N2TOrdersV3Parser.make[SyncIO](spf)

    val res = parser.swap(Output.fromExplorer(V3Orders.deployV3SwapSell)).unsafeRunSync().get

    val rules = new CfmmRuleDefs[SyncIO](MonetaryConfig(10000000, 600, 60000, 10), spf)

    val res2 = rules.rules(res).unsafeRunSync()

    res2 shouldEqual None
  }
}
