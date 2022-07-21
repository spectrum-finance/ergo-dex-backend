package org.ergoplatform.dex.executor.amm

import cats.Functor
import cats.effect.IO
import org.ergoplatform.dex.executor.amm.config.BacklogConfig
import tofu.WithContext

import scala.concurrent.duration._

package object configs {

  val backlogCfgWithNoSuspended = BacklogConfig(
    orderLifetime        = 120.seconds,
    orderExecutionTime   = 10.seconds,
    suspendedOrdersExecutionProbabilityPercent = -1
  )

  val backlogCfgWithOnlySuspended = BacklogConfig(
    orderLifetime        = 120.seconds,
    orderExecutionTime   = 10.seconds,
    suspendedOrdersExecutionProbabilityPercent = 100
  )

  object has {

    val cfgWithNoSuspended: BacklogConfig.Has[IO] = new WithContext[IO, BacklogConfig] {

      override def functor: Functor[IO] = Functor[IO]

      override def context: IO[BacklogConfig] = IO.pure(backlogCfgWithNoSuspended)
    }

    val cfgWithOnlySuspended: BacklogConfig.Has[IO] = new WithContext[IO, BacklogConfig] {

      override def functor: Functor[IO] = Functor[IO]

      override def context: IO[BacklogConfig] = IO.pure(backlogCfgWithOnlySuspended)
    }
  }
}
