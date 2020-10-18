package org.ergoplatform.dex.matcher

import cats.Eval
import org.ergoplatform.dex.domain.models.Trade
import org.scalatest.{Matchers, PropSpec}
import org.ergoplatform.dex.generators._
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class MatchingAlgoSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  property("1 to 1 match, full") {
    val gen =
      for {
        assetX      <- assetIdGen
        assetY      <- assetIdGen
        amount      <- Gen.posNum[Long]
        price       <- Gen.posNum[Long]
        feePerToken <- Gen.chooseNum(100L, 1000L)
        ask         <- askGen(assetX, assetY, amount, price, feePerToken)
        bid         <- bidGen(assetX, assetY, amount, price, feePerToken)
      } yield (ask, bid)
    forAll(gen) { case (ask, bid) =>
      val matcher = MatchingAlgo.instance[Eval]
      val result  = matcher(List(ask), List(bid)).value
      result.foreach { case Trade(order, counterOrders) =>
        counterOrders.toList.foreach { co =>
          co.quoteAsset shouldBe order.quoteAsset
          co.baseAsset shouldBe order.baseAsset
        }
        order.amount shouldBe counterOrders.map(_.amount).toList.sum
      }
    }
  }

  property("1 to many match, full") {
    val gen =
      for {
        assetX      <- assetIdGen
        assetY      <- assetIdGen
        askAmount   <- Gen.chooseNum(1000L, 10000000L)
        bidAmount   <- Gen.chooseNum(100L, 900L)
        price       <- Gen.chooseNum(2L, 1000L)
        feePerToken <- Gen.chooseNum(100L, 1000L)
        ask         <- askGen(assetX, assetY, askAmount, price, feePerToken)
        bid0        <- bidGen(assetX, assetY, bidAmount, price, feePerToken)
        bid1        <- bidGen(assetX, assetY, askAmount - bidAmount, price, feePerToken)
      } yield (ask, bid0, bid1)
    forAll(gen) { case (ask, bid0, bid1) =>
      val matcher = MatchingAlgo.instance[Eval]
      val result  = matcher(List(ask), List(bid0, bid1)).value
      result.foreach { case Trade(order, counterOrders) =>
        counterOrders.toList.foreach { co =>
          co.quoteAsset shouldBe order.quoteAsset
          co.baseAsset shouldBe order.baseAsset
        }
        order.amount shouldBe counterOrders.map(_.amount).toList.sum
      }
    }
  }
}
