package org.ergoplatform.dex.tracker

import cats.effect.{Blocker, ExitCode, Resource}
import fs2.kafka.serde._
import monix.eval.Task
import mouse.any._
import org.ergoplatform.dex.clients.StreamingErgoNetworkClient
import org.ergoplatform.dex.domain.amm.CfmmOperation
import org.ergoplatform.dex.domain.orderbook.Order.AnyOrder
import org.ergoplatform.dex.protocol.amm.AmmContractType.T2tCfmm
import org.ergoplatform.dex.protocol.orderbook.OrderContractFamily.LimitOrders
import org.ergoplatform.dex.streaming.Producer
import org.ergoplatform.dex.tracker.configs.ConfigBundle
import org.ergoplatform.dex.tracker.handlers.{AmmHandler, OrdersHandler}
import org.ergoplatform.dex.tracker.processes.UtxoTracker
import org.ergoplatform.dex.{EnvApp, OperationId, OrderId}
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
      implicit0(isoKRun: IsoK[RunF, InitF]) = IsoK.byFunK(wr.runContextK(configs))(wr.liftF)
      implicit0(producer0: Producer[OrderId, AnyOrder, StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OrderId, AnyOrder](configs.topics.limitOrders, configs.producer)
      implicit0(producer1: Producer[OperationId, CfmmOperation, StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OperationId, CfmmOperation](configs.topics.cfmm, configs.producer)
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(configs, blocker)
      implicit0(client: StreamingErgoNetworkClient[StreamF, RunF]) = StreamingErgoNetworkClient.make[StreamF, RunF]
      limitOrdersHandler <- Resource.eval(OrdersHandler.make[LimitOrders, InitF, StreamF, RunF])
      t2tCfmmHandler     <- Resource.eval(AmmHandler.make[T2tCfmm, InitF, StreamF, RunF])
      tracker            <- Resource.eval(UtxoTracker.make[InitF, StreamF, RunF](limitOrdersHandler, t2tCfmmHandler))
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
