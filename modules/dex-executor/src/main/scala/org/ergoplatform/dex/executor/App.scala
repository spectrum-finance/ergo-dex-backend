package org.ergoplatform.dex.executor

import cats.effect.{ExitCode, Resource}
import fs2.{Chunk, Stream}
import monix.eval.{Task, TaskApp}
import org.ergoplatform.dex.TradeId
import org.ergoplatform.dex.clients.ErgoNetworkClient
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.executor.config.ConfigBundle
import org.ergoplatform.dex.executor.processes.OrdersExecutor
import org.ergoplatform.dex.executor.services.ExecutionService
import org.ergoplatform.dex.streaming.{Consumer, MakeKafkaConsumer}
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import tofu.WithRun
import tofu.env.Env
import tofu.fs2Instances._
import tofu.logging.{LoggableContext, Logs}
import tofu.syntax.monadic._

object App extends TaskApp {

  type InitF[+A]   = Task[A]
  type AppF[+A]    = Env[ConfigBundle, A]
  type StreamF[+A] = Stream[AppF, A]

  implicit private def logs: Logs[InitF, AppF]                = Logs.withContext[InitF, AppF]
  implicit private def loggableContext: LoggableContext[AppF] = LoggableContext.of[AppF].instance[ConfigBundle]

  def run(args: List[String]): Task[ExitCode] =
    init(args.headOption).use { case (executor, ctx) =>
      val appF = executor.run.compile.drain
      appF.run(ctx) as ExitCode.Success
    }

  private def init(configPathOpt: Option[String]): Resource[InitF, (OrdersExecutor[StreamF], ConfigBundle)] =
    for {
      configs <- Resource.liftF(ConfigBundle.load(configPathOpt))
      implicit0(mc: MakeKafkaConsumer[AppF, TradeId, AnyTrade])       = MakeKafkaConsumer.make[InitF, AppF, TradeId, AnyTrade]
      implicit0(consumer: Consumer[TradeId, AnyTrade, StreamF, AppF]) = Consumer.make[StreamF, AppF, TradeId, AnyTrade]
      implicit0(backend: SttpBackend[AppF, Any]) <-
        AsyncHttpClientCatsBackend.resource[AppF]().mapK(WithRun[AppF, InitF, ConfigBundle].runContextK(configs))
      implicit0(client: ErgoNetworkClient[AppF]) = ErgoNetworkClient.make[AppF]
      implicit0(service: ExecutionService[AppF]) <- Resource.liftF(ExecutionService.make[InitF, AppF])
      executor                                   <- Resource.liftF(OrdersExecutor.make[InitF, StreamF, AppF, Chunk])
    } yield executor -> configs
}
