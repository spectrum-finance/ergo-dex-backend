package org.ergoplatform.dex.executor.amm

import cats.effect.{Blocker, ExitCode, Resource}
import fs2.Chunk
import fs2.kafka.serde._
import monix.eval.Task
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.streaming.{Consumer, MakeKafkaConsumer}
import org.ergoplatform.dex.domain.amm.{CFMMOperationRequest, OperationId}
import org.ergoplatform.dex.executor.amm.config.ConfigBundle
import org.ergoplatform.dex.executor.amm.context.AppContext
import org.ergoplatform.dex.executor.amm.interpreters.{CFMMInterpreter, T2TCFMMInterpreter}
import org.ergoplatform.dex.executor.amm.processes.Executor
import org.ergoplatform.dex.executor.amm.repositories.CFMMPools
import org.ergoplatform.dex.executor.amm.services.Execution
import org.ergoplatform.dex.executor.amm.streaming.CFMMConsumer
import org.ergoplatform.dex.protocol.amm.AMMType.T2TCFMM
import org.ergoplatform.ergo.{ErgoNetwork, StreamingErgoNetworkClient}
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

  private def init(configPathOpt: Option[String]): Resource[InitF, (Executor[StreamF], AppContext)] =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load(configPathOpt))
      ctx                              = AppContext.init(configs)
      implicit0(e: ErgoAddressEncoder) = ErgoAddressEncoder(configs.protocol.networkType.prefix)
      implicit0(mc: MakeKafkaConsumer[RunF, OperationId, CFMMOperationRequest]) =
        MakeKafkaConsumer.make[InitF, RunF, OperationId, CFMMOperationRequest]
      //implicit0(isoKRun: IsoK[RunF, InitF])            = IsoK.byFunK(wr.runContextK(ctx))(wr.liftF)
      implicit0(consumer: CFMMConsumer[StreamF, RunF]) = Consumer.make[StreamF, RunF, OperationId, CFMMOperationRequest]
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(ctx, blocker)
      implicit0(client: ErgoNetwork[RunF])                   = StreamingErgoNetworkClient.make[StreamF, RunF]
      implicit0(pools: CFMMPools[RunF])                      = CFMMPools.make[RunF]
      implicit0(interpreter: CFMMInterpreter[T2TCFMM, RunF]) = T2TCFMMInterpreter.make[RunF]
      implicit0(execution: Execution[RunF]) <- Resource.eval(Execution.make[InitF, RunF])
      executor                              <- Resource.eval(Executor.make[InitF, StreamF, RunF, Chunk])
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
