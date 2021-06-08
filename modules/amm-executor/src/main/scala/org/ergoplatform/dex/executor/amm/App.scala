package org.ergoplatform.dex.executor.amm

import cats.effect.{Blocker, ExitCode, Resource}
import fs2.Chunk
import fs2.kafka.serde._
import monix.eval.Task
import org.ergoplatform.common.EnvApp
import org.ergoplatform.dex.domain.amm.{CfmmOperation, OperationId}
import org.ergoplatform.dex.executor.amm.config.ConfigBundle
import org.ergoplatform.dex.executor.amm.context.AppContext
import org.ergoplatform.dex.executor.amm.processes.OrdersExecutor
import streaming.CfmmConsumer
import org.ergoplatform.common.streaming.{Consumer, MakeKafkaConsumer}
import org.ergoplatform.network.{ErgoNetwork, StreamingErgoNetworkClient}
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import tofu.WithRun
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.syntax.unlift._

object App extends EnvApp[AppContext] {

  def run(args: List[String]): Task[ExitCode] =
    init(args.headOption).use { case (executor, ctx) =>
      val appF = executor.run.compile.drain
      appF.run(ctx) as ExitCode.Success
    }

  private def init(configPathOpt: Option[String]): Resource[InitF, (OrdersExecutor[StreamF], AppContext)] =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load(configPathOpt))
      ctx = AppContext.init(configs)
      implicit0(mc: MakeKafkaConsumer[RunF, OperationId, CfmmOperation]) =
        MakeKafkaConsumer.make[InitF, RunF, OperationId, CfmmOperation]
      implicit0(isoKRun: IsoK[RunF, InitF])            = IsoK.byFunK(wr.runContextK(ctx))(wr.liftF)
      implicit0(consumer: CfmmConsumer[StreamF, RunF]) = Consumer.make[StreamF, RunF, OperationId, CfmmOperation]
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(ctx, blocker)
      implicit0(client: ErgoNetwork[RunF]) = StreamingErgoNetworkClient.make[StreamF, RunF]
      executor <- Resource.eval(OrdersExecutor.make[InitF, StreamF, RunF, Chunk])
    } yield executor -> ctx

  private def makeBackend(
    ctx: AppContext,
    blocker: Blocker
  )(implicit wr: WithRun[RunF, InitF, AppContext]): Resource[InitF, SttpBackend[RunF, Fs2Streams[RunF]]] =
    Resource
      .eval(wr.concurrentEffect)
      .flatMap(implicit ce => AsyncHttpClientFs2Backend.resource[RunF](blocker))
      .mapK(wr.runContextK(ctx))
}
