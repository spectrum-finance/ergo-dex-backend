package org.ergoplatform.dex.executor

import cats.effect.{Blocker, ExitCode, Resource}
import fs2.Chunk
import monix.eval.Task
import org.ergoplatform.dex.clients.{ErgoNetworkClient, StreamingErgoNetworkClient}
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.executor.config.ConfigBundle
import org.ergoplatform.dex.executor.processes.OrdersExecutor
import org.ergoplatform.dex.executor.services.ExecutionService
import org.ergoplatform.dex.streaming.{Consumer, MakeKafkaConsumer}
import org.ergoplatform.dex.{EnvApp, TradeId}
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import tofu.WithRun
import tofu.fs2Instances._
import tofu.syntax.unlift._

object App extends EnvApp[ConfigBundle] {

  def run(args: List[String]): Task[ExitCode] =
    init(args.headOption).use { case (executor, ctx) =>
      val appF = executor.run.compile.drain
      appF.run(ctx) as ExitCode.Success
    }

  private def init(configPathOpt: Option[String]): Resource[InitF, (OrdersExecutor[StreamF], ConfigBundle)] =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.liftF(ConfigBundle.load(configPathOpt))
      implicit0(mc: MakeKafkaConsumer[RunF, TradeId, AnyTrade])       = MakeKafkaConsumer.make[InitF, RunF, TradeId, AnyTrade]
      implicit0(consumer: Consumer[TradeId, AnyTrade, StreamF, RunF]) = Consumer.make[StreamF, RunF, TradeId, AnyTrade]
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(configs, blocker)
      implicit0(client: ErgoNetworkClient[RunF]) = StreamingErgoNetworkClient.make[StreamF, RunF]
      implicit0(service: ExecutionService[RunF]) <- Resource.liftF(ExecutionService.make[InitF, RunF])
      executor                                   <- Resource.liftF(OrdersExecutor.make[InitF, StreamF, RunF, Chunk])
    } yield executor -> configs

  private def makeBackend(
    configs: ConfigBundle,
    blocker: Blocker
  )(implicit wr: WithRun[RunF, InitF, ConfigBundle]): Resource[InitF, SttpBackend[RunF, Fs2Streams[RunF]]] =
    Resource
      .liftF(wr.concurrentEffect)
      .flatMap(implicit ce => AsyncHttpClientFs2Backend.resource[RunF](blocker))
      .mapK(wr.runContextK(configs))
}
