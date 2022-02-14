package org.ergoplatform.dex.executor.amm

import cats.effect.{Blocker, Resource}
import fs2.kafka.RecordDeserializer
import fs2.kafka.serde._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.streaming._
import org.ergoplatform.dex.configs.ConsumerConfig
import org.ergoplatform.dex.domain.amm.{CFMMOrder, EvaluatedCFMMOrder, OrderId}
import org.ergoplatform.dex.executor.amm.config.ConfigBundle
import org.ergoplatform.dex.executor.amm.context.AppContext
import org.ergoplatform.dex.executor.amm.interpreters.{CFMMInterpreter, N2TCFMMInterpreter, T2TCFMMInterpreter}
import org.ergoplatform.dex.executor.amm.modules.CFMMBacklog
import org.ergoplatform.dex.executor.amm.processes.{Cleaner, Executor, Registerer}
import org.ergoplatform.dex.executor.amm.repositories.CFMMPools
import org.ergoplatform.dex.executor.amm.services.Execution
import org.ergoplatform.dex.executor.amm.streaming.{CFMMOrders, CFMMOrdersGen, EvaluatedCFMMOrders}
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
import tofu.syntax.unlift._
import zio.interop.catz._
import zio.{ExitCode, URIO, ZEnv}

object App extends EnvApp[AppContext] {

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    init(args.headOption).use { case (executor, registerer, cleaner, ctx) =>
      val appF = fs2.Stream(executor.run, registerer.run, cleaner.run).parJoinUnbounded.compile.drain
      appF.run(ctx) as ExitCode.success
    }.orDie

  private def init(configPathOpt: Option[String]) =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load[InitF](configPathOpt, blocker))
      ctx                                   = AppContext.init(configs)
      implicit0(e: ErgoAddressEncoder)      = ErgoAddressEncoder(configs.protocol.networkType.prefix)
      implicit0(confirmedOrders: CFMMOrdersGen[StreamF, RunF, Confirmed]) =
        makeConsumer[OrderId, Confirmed[CFMMOrder]](configs.consumers.confirmedOrders)
      implicit0(unconfirmedOrders: CFMMOrdersGen[StreamF, RunF, Unconfirmed]) =
        makeConsumer[OrderId, Unconfirmed[CFMMOrder]](configs.consumers.unconfirmedOrders)
      implicit0(orders: CFMMOrders[StreamF, RunF]) =
        Consumer.combine2(confirmedOrders, unconfirmedOrders)(_.entity, _.entity)
      implicit0(evaluatedOrders: EvaluatedCFMMOrders[StreamF, RunF]) =
        makeConsumer[OrderId, EvaluatedCFMMOrder.Any](configs.consumers.evaluatedOrders)
      implicit0(backlog: CFMMBacklog[RunF])                   <- Resource.eval(CFMMBacklog.make[InitF, RunF])
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(ctx, blocker)
      implicit0(explorer: ErgoExplorer[RunF]) = ErgoExplorerStreaming.make[StreamF, RunF]
      implicit0(node: ErgoNode[RunF]) <- Resource.eval(ErgoNode.make[InitF, RunF])
      implicit0(network: ErgoNetwork[RunF]) = ErgoNetwork.make[RunF]
      implicit0(pools: CFMMPools[RunF])                  <- Resource.eval(CFMMPools.make[InitF, RunF])
      implicit0(t2tInt: CFMMInterpreter[T2T_CFMM, RunF]) <- Resource.eval(T2TCFMMInterpreter.make[InitF, RunF])
      implicit0(n2tInt: CFMMInterpreter[N2T_CFMM, RunF]) <- Resource.eval(N2TCFMMInterpreter.make[InitF, RunF])
      implicit0(interpreter: CFMMInterpreter[CFMMType, RunF]) = CFMMInterpreter.make[RunF]
      implicit0(execution: Execution[RunF]) <- Resource.eval(Execution.make[InitF, RunF])
      executor                              <- Resource.eval(Executor.make[InitF, StreamF, RunF])
      registerer                            <- Resource.eval(Registerer.make[InitF, StreamF, RunF])
      cleaner                               <- Resource.eval(Cleaner.make[InitF, StreamF, RunF])
    } yield (executor, registerer, cleaner, ctx)

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
