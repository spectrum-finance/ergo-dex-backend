package org.ergoplatform.dex.matcher

import cats.Eval
import org.ergoplatform.dex.domain.models.Trade
import org.ergoplatform.dex.generators._
import org.ergoplatform.dex.matcher.modules.MatchingAlgo
import org.scalacheck.Gen
import org.scalatest.{Matchers, PropSpec}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class MatchingAlgoSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  property("1 to 1 match, full") {
    val gen =
      for {
        assetX      <- assetIdGen
        assetY      <- assetIdGen
        amount      <- Gen.posNum[Long]
        price       <- priceGen
        feePerToken <- feeGen
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

  property("1 to 1 match, partial") {
    val gen =
      for {
        assetX    <- assetIdGen
        assetY    <- assetIdGen
        bidAmount <- Gen.posNum[Long]
        delta     <- Gen.posNum[Long]
        askAmount = bidAmount + delta
        price       <- priceGen
        feePerToken <- feeGen
        ask         <- askGen(assetX, assetY, askAmount, price, feePerToken)
        bid         <- bidGen(assetX, assetY, bidAmount, price, feePerToken)
      } yield (ask, bid, delta)
    forAll(gen) { case (ask, bid, delta) =>
      val matcher = MatchingAlgo.instance[Eval]
      val result  = matcher(List(ask), List(bid)).value
      result.foreach { case Trade(order, counterOrders) =>
        counterOrders.toList.foreach { co =>
          co.quoteAsset shouldBe order.quoteAsset
          co.baseAsset shouldBe order.baseAsset
        }
        val counterOrdersAmount = counterOrders.map(_.amount).toList.sum
        order.amount - delta shouldBe counterOrdersAmount
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
        price       <- priceGen
        feePerToken <- feeGen
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

  property("1 to many match, partial, one overlaps many") {
    val gen =
      for {
        assetX      <- assetIdGen
        assetY      <- assetIdGen
        askAmount   <- Gen.chooseNum(4000L, 10000000L)
        bidAmount0  <- Gen.chooseNum(1L, 1000L)
        bidAmount1  <- Gen.chooseNum(1L, 1000L)
        price       <- priceGen
        feePerToken <- feeGen
        ask         <- askGen(assetX, assetY, askAmount, price, feePerToken)
        bid0        <- bidGen(assetX, assetY, bidAmount0, price, feePerToken)
        bid1        <- bidGen(assetX, assetY, bidAmount1, price, feePerToken)
        delta = askAmount - bidAmount0 - bidAmount1
      } yield (ask, bid0, bid1, delta)
    forAll(gen) { case (ask, bid0, bid1, delta) =>
      val matcher = MatchingAlgo.instance[Eval]
      val result  = matcher(List(ask), List(bid0, bid1)).value
      result.foreach { case Trade(order, counterOrders) =>
        counterOrders.toList.foreach { co =>
          co.quoteAsset shouldBe order.quoteAsset
          co.baseAsset shouldBe order.baseAsset
        }
        val counterOrdersAmount = counterOrders.map(_.amount).toList.sum
        order.amount - delta shouldBe counterOrdersAmount
      }
    }
  }

  property("1 to many match, partial, many overlaps one") {
    val gen =
      for {
        assetX      <- assetIdGen
        assetY      <- assetIdGen
        askAmount   <- Gen.const(1000)
        bidAmount0  <- Gen.const(500)
        bidAmount1  <- Gen.const(600)
        price       <- priceGen
        feePerToken <- feeGen
        ask         <- askGen(assetX, assetY, askAmount, price, feePerToken)
        bid0        <- bidGen(assetX, assetY, bidAmount0, price, feePerToken)
        bid1        <- bidGen(assetX, assetY, bidAmount1, price, feePerToken)
        delta = bidAmount0 + bidAmount1 - askAmount
      } yield (ask, bid0, bid1, delta)
    forAll(gen) { case (ask, bid0, bid1, delta) =>
      val matcher = MatchingAlgo.instance[Eval]
      val result  = matcher(List(ask), List(bid0, bid1)).value
      result.foreach { case Trade(order, counterOrders) =>
        counterOrders.toList.foreach { co =>
          co.quoteAsset shouldBe order.quoteAsset
          co.baseAsset shouldBe order.baseAsset
        }
        val counterOrdersAmount = counterOrders.map(_.amount).toList.sum
        counterOrdersAmount - delta shouldBe order.amount
      }
    }
  }
}
