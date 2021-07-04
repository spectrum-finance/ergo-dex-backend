package org.ergoplatform.dex.tracker

import cats.effect.{Blocker, ExitCode, Resource}
import fs2.kafka.serde._
import monix.eval.Task
import mouse.any._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.cache.Redis
import org.ergoplatform.common.streaming.Producer
import org.ergoplatform.dex.domain.amm.state.Confirmed
import org.ergoplatform.dex.domain.amm.{CFMMOperationRequest, CFMMPool, OperationId, PoolId}
import org.ergoplatform.dex.protocol.amm.AMMType.T2TCFMM
import org.ergoplatform.dex.tracker.configs.ConfigBundle
import org.ergoplatform.dex.tracker.handlers.{CFMMOpsHandler, CFMMPoolsHandler}
import org.ergoplatform.dex.tracker.processes.UtxoTracker
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
import tofu.logging.derivation.loggable.generate
import tofu.syntax.embed._
import tofu.syntax.unlift._

object App extends EnvApp[ConfigBundle] {

  implicit private def makeRef: MakeRef[InitF, RunF] = MakeRef.syncInstance

  def run(args: List[String]): Task[ExitCode] =
    init(args.headOption).use { case (tracker, ctx) =>
      val appF = tracker.run.compile.drain
      appF.run(ctx) as ExitCode.Success
    }

  private def init(configPathOpt: Option[String]): Resource[InitF, (UtxoTracker[StreamF], ConfigBundle)] =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load(configPathOpt))
      implicit0(e: ErgoAddressEncoder)      = configs.protocol.networkType.addressEncoder
      implicit0(isoKRun: IsoK[RunF, InitF]) = IsoK.byFunK(wr.runContextK(configs))(wr.liftF)
//      implicit0(producer0: Producer[OrderId, AnyOrder, StreamF]) <-
//        Producer.make[InitF, StreamF, RunF, OrderId, AnyOrder](configs.ordersProducer)
      implicit0(producer1: Producer[OperationId, CFMMOperationRequest, StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OperationId, CFMMOperationRequest](configs.cfmmOpsProducer)
      implicit0(producer2: Producer[PoolId, Confirmed[CFMMPool], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, PoolId, Confirmed[CFMMPool]](configs.cfmmPoolsProducer)
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(configs, blocker)
      implicit0(client: ErgoNetworkStreaming[StreamF, RunF]) = ErgoNetworkStreaming.make[StreamF, RunF]
      implicit0(cfmmRules: CFMMRules[RunF])                        = CFMMRules.make[RunF]
      //limitOrdersHandler <- Resource.eval(OrdersHandler.make[LimitOrders, InitF, StreamF, RunF])
      t2tCfmmHandler                       <- Resource.eval(CFMMOpsHandler.make[T2TCFMM, InitF, StreamF, RunF])
      cfmmPoolsHandler                     <- Resource.eval(CFMMPoolsHandler.make[T2TCFMM, InitF, StreamF, RunF])
      implicit0(redis: Redis.Plain[RunF])  <- Redis.make[InitF, RunF](configs.redis)
      implicit0(cache: TrackerCache[RunF]) <- Resource.eval(TrackerCache.make[InitF, RunF])
      tracker <-
        Resource.eval(UtxoTracker.make[InitF, StreamF, RunF]( /*limitOrdersHandler,*/ t2tCfmmHandler, cfmmPoolsHandler))
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
