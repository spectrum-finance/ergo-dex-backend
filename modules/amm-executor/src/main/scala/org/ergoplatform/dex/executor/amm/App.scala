package org.ergoplatform.dex.executor.amm

import cats.Id
import cats.effect.{Blocker, Resource}
import fs2.kafka.RecordDeserializer
import fs2.kafka.serde._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.streaming._
import org.ergoplatform.dex.configs.ConsumerConfig
import org.ergoplatform.dex.domain.amm.{CFMMOrder, OrderId}
import org.ergoplatform.dex.executor.amm.config.ConfigBundle
import org.ergoplatform.dex.executor.amm.context.AppContext
import org.ergoplatform.dex.executor.amm.interpreters.v1.{InterpreterV1, N2TCFMMInterpreter, T2TCFMMInterpreter}
import org.ergoplatform.dex.executor.amm.interpreters.v3.InterpreterV3
import org.ergoplatform.dex.executor.amm.interpreters.v3.n2t.N2TV3
import org.ergoplatform.dex.executor.amm.interpreters.v3.t2t.T2TV3
import org.ergoplatform.dex.executor.amm.interpreters.CFMMInterpreter
import org.ergoplatform.dex.executor.amm.processes.{Executor, NetworkContextUpdater}
import org.ergoplatform.dex.executor.amm.repositories.CFMMPools
import org.ergoplatform.dex.executor.amm.services.{DexOutputResolver, Execution}
import org.ergoplatform.dex.executor.amm.streaming._
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.ergo.modules.ErgoNetwork
import org.ergoplatform.ergo.services.explorer.{ErgoExplorer, ErgoExplorerStreaming}
import org.ergoplatform.ergo.services.node.ErgoNode
import org.ergoplatform.ergo.state.{Confirmed, Unconfirmed}
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import tofu.WithRun
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.syntax.unlift._
import zio.interop.catz._
import zio.{ExitCode, URIO, ZEnv}

object App extends EnvApp[AppContext] {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    init(args.headOption).use { case (tasks, ctx) =>
      val appF = fs2.Stream(tasks: _*).parJoinUnbounded.compile.drain
      appF.run(ctx) as ExitCode.success
    }.orDie

  private def init(configPathOpt: Option[String]) =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load[InitF](configPathOpt, blocker))
      ctx                                   = AppContext.init(configs)
      implicit0(isoKRun: IsoK[RunF, InitF]) = isoKRunByContext(ctx)
      implicit0(e: ErgoAddressEncoder)      = ErgoAddressEncoder(configs.protocol.networkType.prefix)
      implicit0(confirmedOrders: CFMMConsumerIn[StreamF, RunF, Confirmed]) =
        makeConsumer[OrderId, Confirmed[CFMMOrder.AnyOrder]](configs.consumers.confirmedOrders)
      implicit0(unconfirmedOrders: CFMMConsumerIn[StreamF, RunF, Unconfirmed]) =
        makeConsumer[OrderId, Unconfirmed[CFMMOrder.AnyOrder]](configs.consumers.unconfirmedOrders)
      implicit0(consumerRetries: CFMMConsumerRetries[StreamF, RunF]) =
        makeConsumer[OrderId, Delayed[CFMMOrder.AnyOrder]](configs.consumers.ordersRetry)
      implicit0(orders: CFMMConsumerIn[StreamF, RunF, Id]) =
        Consumer.combine2(confirmedOrders, unconfirmedOrders)(_.entity, _.entity)
      implicit0(producerRetries: CFMMProducerRetries[StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OrderId, Delayed[CFMMOrder.AnyOrder]](configs.producers.ordersRetry)
      implicit0(consumer: CFMMCircuit[StreamF, RunF]) =
        StreamingCircuit.make[StreamF, RunF, OrderId, CFMMOrder.AnyOrder]
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(ctx, blocker)
      implicit0(explorer: ErgoExplorer[RunF]) = ErgoExplorerStreaming.make[StreamF, RunF]
      implicit0(node: ErgoNode[RunF]) <- Resource.eval(ErgoNode.make[InitF, RunF])
      implicit0(network: ErgoNetwork[RunF]) = ErgoNetwork.make[RunF]
      implicit0(pools: CFMMPools[RunF]) <- Resource.eval(CFMMPools.make[InitF, RunF])
      (networkContextUpdater, context)  <- Resource.eval(NetworkContextUpdater.make[InitF, StreamF, RunF])
      implicit0(resolver: DexOutputResolver[RunF]) <-
        Resource.eval(DexOutputResolver.make[InitF, RunF](configs.exchange))
      implicit0(t2tInt: InterpreterV1[T2T_CFMM, RunF]) <-
        Resource.eval(T2TCFMMInterpreter.make[InitF, RunF])
      implicit0(n2tInt: InterpreterV1[N2T_CFMM, RunF]) <-
        Resource.eval(N2TCFMMInterpreter.make[InitF, RunF])
      implicit0(n2tInt: InterpreterV3[N2T_CFMM, RunF]) <-
        Resource.eval(N2TV3.make[InitF, RunF](configs.exchange, configs.monetary, context))
      implicit0(n2tInt: InterpreterV3[T2T_CFMM, RunF]) <-
        Resource.eval(T2TV3.make[InitF, RunF](configs.exchange, configs.monetary, context))
      implicit0(interpreter: CFMMInterpreter[CFMMType, RunF]) = CFMMInterpreter.make[RunF]
      implicit0(execution: Execution[RunF]) <- Resource.eval(Execution.make[InitF, RunF])
      executor                              <- Resource.eval(Executor.make[InitF, StreamF, RunF])
    } yield List(executor.run, networkContextUpdater.run) -> ctx

  private def makeBackend(
    ctx: AppContext,
    blocker: Blocker
  )(implicit wr: WithRun[RunF, InitF, AppContext]): Resource[InitF, SttpBackend[RunF, Fs2Streams[RunF]]] =
    Resource
      .eval(wr.concurrentEffect)
      .flatMap(implicit ce => AsyncHttpClientFs2Backend.resource[RunF](blocker))
      .mapK(wr.runContextK(ctx))

  private def makeConsumer[K: RecordDeserializer[RunF, *], V: RecordDeserializer[RunF, *]](conf: ConsumerConfig) = {
    implicit val maker = MakeKafkaConsumer.make[InitF, RunF, K, V]
    Consumer.make[StreamF, RunF, K, V](conf)
  }
}
