package org.ergoplatform.dex.executor.modules

import cats.Eval
import cats.data.{NonEmptyList, ReaderT}
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.BoxId
import org.ergoplatform.dex.configs.ProtocolConfig
import org.ergoplatform.dex.domain.models.Trade
import org.ergoplatform.dex.domain.syntax.ergo._
import org.ergoplatform.dex.domain.syntax.trade._
import org.ergoplatform.dex.executor.config.{ConfigBundle, ExchangeConfig}
import org.ergoplatform.dex.executor.context.BlockchainContext
import org.ergoplatform.dex.generators._
import org.scalacheck.Gen
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sigmastate.Values._

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
      val exConf = ExchangeConfig(rewardAddress)
      val protoConf = ProtocolConfig(ErgoAddressEncoder.MainnetNetworkPrefix)
      val blockchainContext = BlockchainContext(curHeight = 100)
      val ctx               = TestCtx(exConf, protoConf, blockchainContext)
      val tx                = Transactions[ReaderT[Eval, TestCtx, *]].translate(trade).run(ctx).value
      tx.inputs.map(i => BoxId.fromErgo(i.boxId)) should contain theSameElementsAs trade.orders.map(_.meta.boxId).toList
      trade.orders.toList.foreach {
        case order if order.`type`.isAsk =>
          val orderReward = tx.outputCandidates.find { o =>
            o.value == order.amount * order.price && o.ergoTree == ErgoTree.fromSigmaBoolean(order.meta.pk)
          }
          orderReward should not be empty
        case order =>
          val orderReward = tx.outputCandidates.find { o =>
            o.additionalTokens.toMap
              .find { case (id, _) => java.util.Arrays.equals(id, order.quoteAsset.toErgo) }
              .map(_._2)
              .contains(order.amount) &&
            o.ergoTree == ErgoTree.fromSigmaBoolean(order.meta.pk)
          }
          orderReward should not be empty
      }
    }
  }
}
