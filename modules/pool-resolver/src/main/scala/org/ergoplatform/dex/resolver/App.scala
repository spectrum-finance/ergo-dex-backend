package org.ergoplatform.dex.resolver

import cats.effect.{ExitCode, Resource}
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

object App extends EnvApp[AppContext] {

  def run(args: List[String]): InitF[ExitCode] =
    init(args.headOption).use { case (tracker, server, ctx) =>
      Stream(tracker.run.translate(runContextK[RunF][AppContext, InitF](ctx)), server).parJoinUnbounded.compile.drain
        .as(ExitCode.Success)
    }

  private def init(configPathOpt: Option[String]) =
    for {
      configs <- Resource.eval(ConfigBundle.load(configPathOpt))
      ctx = AppContext.init(configs)
      implicit0(mc: MakeKafkaConsumer[RunF, PoolId, Confirmed[CFMMPool]]) =
        MakeKafkaConsumer.make[InitF, RunF, PoolId, Confirmed[CFMMPool]]
      implicit0(ul: Unlift[RunF, InitF]) = Unlift.byIso(IsoK.byFunK(wr.runContextK(ctx))(wr.liftF))
      implicit0(consumer: Consumer[PoolId, Confirmed[CFMMPool], StreamF, RunF]) =
        Consumer.make[StreamF, RunF, PoolId, Confirmed[CFMMPool]]
      implicit0(pools: Pools[RunF])       <- Resource.eval(Pools.make[InitF, RunF])
      implicit0(resolver: Resolver[RunF]) <- Resource.eval(Resolver.make[InitF, RunF])
      tracker = PoolTracker.make[StreamF, RunF]
      server  = HttpServer.make[InitF, RunF](configs.http, scheduler)
    } yield (tracker, server, ctx)
}
