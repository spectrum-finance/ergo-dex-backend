package org.ergoplatform.dex.executor.amm

import cats.effect.{Blocker, Resource}
import fs2.kafka.serde._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.streaming._
import org.ergoplatform.dex.domain.amm.{CFMMOrder, OrderId}
import org.ergoplatform.dex.executor.amm.config.ConfigBundle
import org.ergoplatform.dex.executor.amm.context.AppContext
import org.ergoplatform.dex.executor.amm.interpreters.{CFMMInterpreter, T2TCFMMInterpreter}
import org.ergoplatform.dex.executor.amm.processes.Executor
import org.ergoplatform.dex.executor.amm.repositories.CFMMPools
import org.ergoplatform.dex.executor.amm.services.Execution
import org.ergoplatform.dex.executor.amm.streaming.{
  CFMMCircuit,
  CFMMConsumerIn,
  CFMMConsumerRetries,
  CFMMProducerRetries
}
import org.ergoplatform.dex.protocol.amm.AMMType.T2TCFMM
import org.ergoplatform.ergo.ErgoNetwork
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import tofu.WithRun
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.syntax.unlift._
import zio.interop.catz._
import zio.{ExitCode, URIO, ZEnv}

object App extends EnvApp[AppContext] {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    init(args.headOption).use { case (executor, ctx) =>
      val appF = executor.run.compile.drain
      appF.run(ctx) as ExitCode.success
    }.orDie

  private def init(configPathOpt: Option[String]): Resource[InitF, (Executor[StreamF], AppContext)] =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load[InitF](configPathOpt, blocker))
      ctx                                   = AppContext.init(configs)
      implicit0(isoKRun: IsoK[RunF, InitF]) = isoKRunByContext(ctx)
      implicit0(e: ErgoAddressEncoder)      = ErgoAddressEncoder(configs.protocol.networkType.prefix)
      implicit0(mkc: MakeKafkaConsumer[RunF, OrderId, CFMMOrder]) =
        MakeKafkaConsumer.make[InitF, RunF, OrderId, CFMMOrder]
      implicit0(mkcd: MakeKafkaConsumer[RunF, OrderId, Delayed[CFMMOrder]]) =
        MakeKafkaConsumer.make[InitF, RunF, OrderId, Delayed[CFMMOrder]]
      implicit0(consumerIn: CFMMConsumerIn[StreamF, RunF]) =
        Consumer.make[StreamF, RunF, OrderId, CFMMOrder](configs.orders)
      implicit0(consumerRetries: CFMMConsumerRetries[StreamF, RunF]) =
        Consumer.make[StreamF, RunF, OrderId, Delayed[CFMMOrder]](configs.ordersRetryIn)
      implicit0(producerRetries: CFMMProducerRetries[StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OrderId, Delayed[CFMMOrder]](configs.ordersRetryOut)
      implicit0(consumer: CFMMCircuit[StreamF, RunF]) = StreamingCircuit.make[StreamF, RunF, OrderId, CFMMOrder]
      implicit0(backend: SttpBackend[RunF, Any])             <- makeBackend(ctx)
      implicit0(client: ErgoNetwork[RunF])                   <- Resource.eval(ErgoNetwork.make[InitF, RunF])
      implicit0(pools: CFMMPools[RunF])                      <- Resource.eval(CFMMPools.make[InitF, RunF])
      implicit0(interpreter: CFMMInterpreter[T2TCFMM, RunF]) <- Resource.eval(T2TCFMMInterpreter.make[InitF, RunF])
      implicit0(execution: Execution[RunF])                  <- Resource.eval(Execution.make[InitF, RunF])
      executor                                               <- Resource.eval(Executor.make[InitF, StreamF, RunF])
    } yield executor -> ctx

  private def makeBackend(
    ctx: AppContext
  )(implicit wr: WithRun[RunF, InitF, AppContext]): Resource[InitF, SttpBackend[RunF, Any]] =
    Resource
      .eval(wr.concurrentEffect)
      .flatMap(implicit ce => AsyncHttpClientCatsBackend.resource[RunF]())
      .mapK(wr.runContextK(ctx))
}
