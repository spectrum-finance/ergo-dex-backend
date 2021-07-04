package org.ergoplatform.dex.resolver

import cats.effect.{Blocker, Resource}
import fs2.Stream
import fs2.kafka.serde._
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.streaming.{Consumer, MakeKafkaConsumer}
import org.ergoplatform.dex.domain.amm.state.Confirmed
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.resolver.config.ConfigBundle
import org.ergoplatform.dex.resolver.http.HttpServer
import org.ergoplatform.dex.resolver.processes.PoolTracker
import org.ergoplatform.dex.resolver.repositories.Pools
import org.ergoplatform.dex.resolver.services.Resolver
import tofu.fs2Instances._
import tofu.lift.{IsoK, Unlift}
import tofu.syntax.context._
import zio.interop.catz._
import zio.{ExitCode, URIO, ZEnv}

object App extends EnvApp[AppContext] {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    init(args.headOption).use { case (tracker, server, ctx) =>
      Stream(tracker.run.translate(runContextK[RunF][AppContext, InitF](ctx)), server).parJoinUnbounded.compile.drain
        .as(ExitCode.success)
    }.orDie

  private def init(configPathOpt: Option[String]) =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load[InitF](configPathOpt, blocker))
      ctx = AppContext.init(configs)
      implicit0(mc: MakeKafkaConsumer[RunF, PoolId, Confirmed[CFMMPool]]) =
        MakeKafkaConsumer.make[InitF, RunF, PoolId, Confirmed[CFMMPool]]
      implicit0(ul: Unlift[RunF, InitF]) = Unlift.byIso(IsoK.byFunK(wr.runContextK(ctx))(wr.liftF))
      implicit0(consumer: Consumer[PoolId, Confirmed[CFMMPool], StreamF, RunF]) =
        Consumer.make[StreamF, RunF, PoolId, Confirmed[CFMMPool]]
      implicit0(pools: Pools[RunF])       <- Resource.eval(Pools.make[InitF, RunF])
      implicit0(resolver: Resolver[RunF]) <- Resource.eval(Resolver.make[InitF, RunF])
      tracker = PoolTracker.make[StreamF, RunF]
      server  = HttpServer.make[InitF, RunF](configs.http, runtime.platform.executor.asEC)
    } yield (tracker, server, ctx)
}
