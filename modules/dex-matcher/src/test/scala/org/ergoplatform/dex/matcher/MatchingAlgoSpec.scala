package org.ergoplatform.dex.matcher

import cats.Eval
import org.scalatest.{Matchers, PropSpec}
import org.ergoplatform.dex.generators._
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class MatchingAlgoSpec extends PropSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  property("Simple match") {
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
      result.isEmpty shouldBe false
      println(result)
    }
  }
}
