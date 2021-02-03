package org.ergoplatform.dex.executor.modules

import cats.Eval
import cats.data.{NonEmptyList, ReaderT}
import cats.effect.IO
import org.ergoplatform.dex.BoxId
import org.ergoplatform.dex.configs.ProtocolConfig
import org.ergoplatform.dex.domain.models.{FilledOrder, Trade}
import org.ergoplatform.dex.domain.syntax.ergo._
import org.ergoplatform.dex.executor.config.ExchangeConfig
import org.ergoplatform.dex.executor.context.BlockchainContext
import org.ergoplatform.dex.generators._
import org.ergoplatform.dex.protocol.Network
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
        amount      <- Gen.const(10000L)
        price       <- Gen.const(1000L)
        feePerToken <- Gen.const(100L)
        ask         <- askGen(assetX, assetY, amount, price, feePerToken)
        bid         <- bidGen(assetX, assetY, amount, price, feePerToken)
      } yield Trade(FilledOrder(ask, ask.price), NonEmptyList.one(FilledOrder(bid, bid.price)))
    forAll(tradeGen, addressGen) { case (trade, rewardAddress) =>
      val exConf            = ExchangeConfig(rewardAddress)
      val protoConf         = ProtocolConfig(Network.MainNet)
      val blockchainContext = BlockchainContext(currentHeight = 100, nanoErgsPerByte = 1L)
      val ctx               = TestCtx(exConf, protoConf, blockchainContext)
      val tx                = Transactions.instance[ReaderT[IO, TestCtx, *]].translate(trade).run(ctx).unsafeRunSync()
      tx.inputs.map(i => BoxId.fromErgo(i.boxId)) should contain theSameElementsAs trade.orders.map(_.base.meta.boxId).toList
      trade.orders.toList.foreach {
        case order if order.base.`type`.isAsk =>
          val orderReward = tx.outputCandidates.find { o =>
            o.value == order.base.amount * order.executionPrice && o.ergoTree == ErgoTree.fromSigmaBoolean(order.base.meta.pk)
          }
          orderReward should not be empty
        case order =>
          val orderReward = tx.outputCandidates.find { o =>
            o.additionalTokens.toMap
              .find { case (id, _) => java.util.Arrays.equals(id, order.base.quoteAsset.toErgo) }
              .map(_._2)
              .contains(order.base.amount) &&
            o.ergoTree == ErgoTree.fromSigmaBoolean(order.base.meta.pk)
          }
          orderReward should not be empty
      }
    }
  }
}
