package org.ergoplatform.dex.tracker

import cats.effect.{Blocker, Resource}
import fs2.kafka.serde._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.cache.{MakeRedisTransaction, Redis}
import org.ergoplatform.common.streaming.Producer
import org.ergoplatform.dex.domain.amm.state.Confirmed
import org.ergoplatform.dex.domain.amm.{CFMMOrder, CFMMPool, OrderId, PoolId}
import org.ergoplatform.dex.domain.locks.LiquidityLock
import org.ergoplatform.dex.domain.locks.types.LockId
import org.ergoplatform.dex.tracker.configs.ConfigBundle
import org.ergoplatform.dex.tracker.handlers.{CFMMOpsHandler, CFMMPoolsHandler, LiquidityLocksHandler}
import org.ergoplatform.dex.tracker.processes.UtxoTracker
import org.ergoplatform.dex.tracker.processes.UtxoTracker.TrackerMode
import org.ergoplatform.dex.tracker.repositories.TrackerCache
import org.ergoplatform.dex.tracker.validation.amm.CFMMRules
import org.ergoplatform.ergo.ErgoNetworkStreaming
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import tofu.WithRun
import tofu.concurrent.MakeRef
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.syntax.unlift._
import zio.interop.catz._
import zio.{ExitCode, URIO, ZEnv}

object App extends EnvApp[ConfigBundle] {

  implicit val makeRef: MakeRef[InitF, RunF]   = MakeRef.syncInstance
  implicit val mtx: MakeRedisTransaction[RunF] = MakeRedisTransaction.make[RunF]

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    init(args.headOption).use { case (tracker, ctx) =>
      val appF = tracker.run.compile.drain
      appF.run(ctx) as ExitCode.success
    }.orDie

  private def init(configPathOpt: Option[String]): Resource[InitF, (UtxoTracker[StreamF], ConfigBundle)] =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load[InitF](configPathOpt, blocker))
      implicit0(e: ErgoAddressEncoder)      = configs.protocol.networkType.addressEncoder
      implicit0(isoKRun: IsoK[RunF, InitF]) = isoKRunByContext(configs)
      implicit0(producer1: Producer[OrderId, CFMMOrder, StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OrderId, CFMMOrder](configs.publishers.ammOrders)
      implicit0(producer2: Producer[PoolId, Confirmed[CFMMPool], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, PoolId, Confirmed[CFMMPool]](configs.publishers.ammPools)
      implicit0(producer3: Producer[LockId, Confirmed[LiquidityLock], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, LockId, Confirmed[LiquidityLock]](configs.publishers.lqLocks)
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(configs, blocker)
      implicit0(client: ErgoNetworkStreaming[StreamF, RunF]) = ErgoNetworkStreaming.make[StreamF, RunF]
      implicit0(cfmmRules: CFMMRules[RunF])                  = CFMMRules.make[RunF]
      ammOrderHandler                      <- Resource.eval(CFMMOpsHandler.make[InitF, StreamF, RunF])
      ammPoolsHandler                      <- Resource.eval(CFMMPoolsHandler.make[InitF, StreamF, RunF])
      lqLocksHandler                       <- Resource.eval(LiquidityLocksHandler.make[InitF, StreamF, RunF])
      implicit0(redis: Redis.Plain[RunF])  <- Redis.make[InitF, RunF](configs.redis)
      implicit0(cache: TrackerCache[RunF]) <- Resource.eval(TrackerCache.make[InitF, RunF])
      tracker <-
        Resource.eval(UtxoTracker.make[InitF, StreamF, RunF](TrackerMode.Live, ammOrderHandler, ammPoolsHandler, lqLocksHandler))
    } yield tracker -> configs

  private def makeBackend(
    configs: ConfigBundle,
    blocker: Blocker
  )(implicit wr: WithRun[RunF, InitF, ConfigBundle]): Resource[InitF, SttpBackend[RunF, Fs2Streams[RunF]]] =
    Resource
      .eval(wr.concurrentEffect)
      .flatMap(implicit ce => AsyncHttpClientFs2Backend.resource[RunF](blocker))
      .mapK(wr.runContextK(configs))
}
