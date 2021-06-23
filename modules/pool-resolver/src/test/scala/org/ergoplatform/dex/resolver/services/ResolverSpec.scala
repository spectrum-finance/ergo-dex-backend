package org.ergoplatform.dex.resolver.services

import cats.effect.IO
import cats.instances.list._
import cats.syntax.foldable._
import org.ergoplatform.dex.domain.amm.state.{Confirmed, Predicted}
import org.ergoplatform.dex.generators._
import org.ergoplatform.dex.resolver.repositories.Pools
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import tofu.concurrent.MakeRef
import tofu.logging.Logs
import tofu.syntax.monadic._

class ResolverSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks {

  implicit val logs: Logs[IO, IO]       = Logs.sync[IO, IO]
  implicit val makeRef: MakeRef[IO, IO] = MakeRef.syncInstance[IO, IO]

  def make: IO[(Pools[IO], Resolver[IO])] =
    for {
      implicit0(pools: Pools[IO]) <- Pools.make[IO, IO]
      resolver                    <- Resolver.make[IO, IO]
    } yield pools -> resolver

  property("Trivial resolving") {
    forAll(cfmmPoolGen) { pool =>
      val testResultF = make.flatMap { case (pools, resolver) =>
        pools.put(Confirmed(pool)) >>
          pools.put(Predicted(pool)) >>
          resolver.resolve(pool.poolId)
      }
      val testResult = testResultF.unsafeRunSync()
      testResult shouldBe Some(pool)
    }
  }

  property("Predictions chain resolving") {
    forAll(cfmmPoolPredictionsGen(10)) { predictions =>
      val root = predictions.head
      val last = predictions.last
      val testResultF = make.flatMap { case (pools, resolver) =>
        pools.put(Confirmed(root)) >>
          predictions.traverse_(p => pools.put(Predicted(p))) >>
          resolver.resolve(last.poolId)
      }
      val testResult = testResultF.unsafeRunSync()
      testResult shouldBe Some(last)
    }
  }
}
