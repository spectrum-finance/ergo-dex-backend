package org.ergoplatform.dex.resolver

import cats.effect.{Blocker, ExitCode, Resource}
import fs2.kafka.serde._
import monix.eval.Task
import mouse.any._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.streaming.Producer
import org.ergoplatform.dex.domain.amm.{CFMMOperationRequest, CFMMPool, OperationId, PoolId}
import org.ergoplatform.dex.domain.orderbook.Order.AnyOrder
import org.ergoplatform.dex.domain.orderbook.OrderId
import org.ergoplatform.dex.protocol.amm.AMMType.T2TCFMM
import org.ergoplatform.dex.protocol.orderbook.OrderContractFamily.LimitOrders
import org.ergoplatform.dex.resolver.configs.ConfigBundle
import org.ergoplatform.dex.resolver.handlers.{CFMMOpsHandler, CFMMPoolsHandler, OrdersHandler}
import org.ergoplatform.dex.resolver.processes.UtxoTracker
import org.ergoplatform.dex.resolver.validation.amm.CFMMRules
import org.ergoplatform.ergo.StreamingErgoNetworkClient
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
      implicit0(producer0: Producer[OrderId, AnyOrder, StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OrderId, AnyOrder](configs.ordersProducer)
      implicit0(producer1: Producer[OperationId, CFMMOperationRequest, StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OperationId, CFMMOperationRequest](configs.cfmmProducer)
      implicit0(producer2: Producer[PoolId, CFMMPool, StreamF]) <-
        Producer.make[InitF, StreamF, RunF, PoolId, CFMMPool](configs.cfmmProducer)
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(configs, blocker)
      implicit0(client: StreamingErgoNetworkClient[StreamF, RunF]) = StreamingErgoNetworkClient.make[StreamF, RunF]
      implicit0(cfmmRules: CFMMRules[RunF])                        = CFMMRules.make[RunF]
      limitOrdersHandler <- Resource.eval(OrdersHandler.make[LimitOrders, InitF, StreamF, RunF])
      t2tCfmmHandler     <- Resource.eval(CFMMOpsHandler.make[T2TCFMM, InitF, StreamF, RunF])
      cfmmPoolsHandler   <- Resource.eval(CFMMPoolsHandler.make[T2TCFMM, InitF, StreamF, RunF])
      tracker <-
        Resource.eval(UtxoTracker.make[InitF, StreamF, RunF](limitOrdersHandler, t2tCfmmHandler, cfmmPoolsHandler))
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
