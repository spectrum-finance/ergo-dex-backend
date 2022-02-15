package org.ergoplatform.common.data

import cats.effect.IO
import org.ergoplatform.dex.CatsPlatform
import org.ergoplatform.dex.generators._
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.concurrent.duration._

class TemporalFilterSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks with CatsPlatform {

  property("double probe") {
    forAll(boxIdGen) { box =>
      val test = for {
        f  <- TemporalFilter.make[IO, IO](1.minute, 2)
        p1 <- f.probe(box)
        _ = p1 shouldBe false
        p2 <- f.probe(box)
        _ = p2 shouldBe true
      } yield ()
      test.unsafeRunSync()
    }
  }
}
