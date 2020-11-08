package org.ergoplatform.dex.executor.modules

import cats.Eval
import cats.data.{NonEmptyList, ReaderT}
import cats.syntax.show._
import io.circe.Printer
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.models.Trade
import org.ergoplatform.dex.executor.config.{ConfigBundle, ExchangeConfig, ProtocolConfig}
import org.ergoplatform.dex.executor.context.BlockchainContext
import org.ergoplatform.dex.generators._
import org.ergoplatform.dex.protocol.codecs._
import org.scalacheck.Gen
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tofu.Context
import tofu.optics.macros.{promote, ClassyOptics}

class TransactionsSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks {

  property("transaction assembly") {
    val tradeGen =
      for {
        assetX      <- assetIdGen
        assetY      <- assetIdGen
        amount      <- Gen.const(1000L)
        price       <- Gen.const(100L)
        feePerToken <- Gen.const(10L)
        ask         <- askGen(assetX, assetY, amount, price, feePerToken)
        bid         <- bidGen(assetX, assetY, amount, price, feePerToken)
      } yield Trade(ask, NonEmptyList.one(bid))
    forAll(tradeGen, addressGen) { case (trade, rewardAddress) =>
      val configs           = ConfigBundle(ExchangeConfig(rewardAddress), ProtocolConfig(ErgoAddressEncoder.MainnetNetworkPrefix))
      val blockchainContext = BlockchainContext(curHeight = 100)
      val ctx               = Ctx(configs, blockchainContext)
      val tx                = Transactions[ReaderT[Eval, Ctx, *]].translate(trade).run(ctx).value
      import io.circe.syntax._
      println(trade.show)
      println(tx.asJson.printWith(Printer.spaces4))
    }
  }

  @ClassyOptics
  final case class Ctx(@promote configs: ConfigBundle, @promote blockchain: BlockchainContext)
  object Ctx extends Context.Companion[Ctx]
}
