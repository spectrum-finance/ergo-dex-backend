package org.ergoplatform.dex.resolver

import cats.effect.{Blocker, Resource}
import fs2.Stream
import fs2.kafka.serde._
import io.github.oskin1.rocksdb.scodec.TxRocksDB
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.streaming.{Consumer, MakeKafkaConsumer}
import org.ergoplatform.dex.domain.amm.state.Confirmed
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.resolver.config.ConfigBundle
import org.ergoplatform.dex.resolver.http.HttpServer
import org.ergoplatform.dex.resolver.processes.PoolTracker
import org.ergoplatform.dex.resolver.repositories.CFMMPools
import org.ergoplatform.dex.resolver.services.Resolver
import org.ergoplatform.dex.resolver.AppContext._
import sttp.tapir.server.http4s.Http4sServerOptions
import tofu.fs2Instances._
import tofu.lift.{IsoK, Unlift}
import tofu.syntax.context._
import zio.interop.catz._
import zio.{ExitCode, URIO, ZEnv}

object App extends EnvApp[AppContext] {

  implicit val serverOptions: Http4sServerOptions[RunF, RunF] = Http4sServerOptions.default[RunF, RunF]

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
        Consumer.make[StreamF, RunF, PoolId, Confirmed[CFMMPool]](configs.consumer)
      implicit0(rocks: TxRocksDB[RunF])   <- TxRocksDB.make[InitF, RunF](configs.rocks.path)
      implicit0(pools: CFMMPools[RunF])   <- Resource.eval(CFMMPools.make[InitF, RunF])
      implicit0(resolver: Resolver[RunF]) <- Resource.eval(Resolver.make[InitF, RunF])
      tracker = PoolTracker.make[StreamF, RunF]
      server  = HttpServer.make[InitF, RunF](configs.http, runtime.platform.executor.asEC)
    } yield (tracker, server, ctx)
}
