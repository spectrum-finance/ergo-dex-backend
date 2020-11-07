package org.ergoplatform.dex.executor.modules

import cats.Eval
import cats.data.{NonEmptyList, ReaderT}
import io.circe.Printer
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.models.Trade
import org.ergoplatform.dex.executor.config.ExchangeConfig
import org.ergoplatform.dex.executor.context.TxContext
import org.ergoplatform.dex.generators._
import org.scalacheck.Gen
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class TransactionsSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks {

  property("transaction assembly") {
    val tradeGen =
      for {
        assetX      <- assetIdGen
        assetY      <- assetIdGen
        amount      <- Gen.posNum[Long]
        price       <- priceGen
        feePerToken <- feeGen
        ask         <- askGen(assetX, assetY, amount, price, feePerToken)
        bid         <- bidGen(assetX, assetY, amount, price, feePerToken)
      } yield Trade(ask, NonEmptyList.one(bid))
    implicit val e: ErgoAddressEncoder = new ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)
    val transactions                   = Transactions.instance[ReaderT[Eval, ExchangeConfig, *]]
    forAll(tradeGen, addressGen) { case (trade, rewardAddress) =>
      val config                    = ExchangeConfig(rewardAddress)
      implicit val txCtx: TxContext = TxContext(curHeight = 100)
      val tx                        = transactions.toTransaction(trade).run(config).value
      import io.circe.syntax._
      import org.ergoplatform.dex.protocol.codecs._
      println(tx.asJson.printWith(Printer.spaces4))
    }
  }
}
