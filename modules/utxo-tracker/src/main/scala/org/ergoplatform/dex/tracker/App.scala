package org.ergoplatform.dex.tracker

import cats.effect.{Blocker, Resource}
import fs2.kafka.serde._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.cache.{MakeRedisTransaction, Redis}
import org.ergoplatform.common.streaming.Producer
import org.ergoplatform.dex.domain.amm.{CFMMOrder, CFMMPool, OrderId, PoolId}
import org.ergoplatform.dex.tracker.configs.ConfigBundle
import org.ergoplatform.dex.tracker.handlers.{lift, CFMMOpsHandler, CFMMPoolsHandler, SettledCFMMPoolsHandler}
import org.ergoplatform.dex.tracker.processes.LedgerTracker.TrackerMode
import org.ergoplatform.dex.tracker.processes.{LedgerTracker, MempoolTracker}
import org.ergoplatform.dex.tracker.repositories.TrackerCache
import org.ergoplatform.dex.tracker.validation.amm.CFMMRules
import org.ergoplatform.ergo.modules.{ErgoNetwork, LedgerStreaming, MempoolStreaming}
import org.ergoplatform.ergo.services.explorer.ErgoExplorerStreaming
import org.ergoplatform.ergo.services.node.ErgoNode
import org.ergoplatform.ergo.state.{Confirmed, ConfirmedIndexed, Unconfirmed}
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import tofu.WithRun
import tofu.concurrent.MakeRef
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.syntax.unlift._
import zio.interop.catz._
import zio.{ExitCode, URIO, ZEnv}

object App extends EnvApp[ConfigBundle] {

  implicit val makeRef: MakeRef[InitF, RunF]   = MakeRef.syncInstance
  implicit val mtx: MakeRedisTransaction[RunF] = MakeRedisTransaction.make[RunF]

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    init(args.headOption).use { case (ledgerTracker, mempoolTracker, ctx) =>
      val appF = fs2.Stream(ledgerTracker.run, mempoolTracker.run).parJoinUnbounded.compile.drain
      appF.run(ctx) as ExitCode.success
    }.orDie

  // format: off
  private def init(configPathOpt: Option[String]) =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load[InitF](configPathOpt, blocker))
      implicit0(e: ErgoAddressEncoder)      = configs.protocol.networkType.addressEncoder
      implicit0(isoKRun: IsoK[RunF, InitF]) = isoKRunByContext(configs)
      implicit0(producer1: Producer[OrderId, Confirmed[CFMMOrder], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OrderId, Confirmed[CFMMOrder]](configs.producers.confirmedAmmOrders)
      implicit0(producer2: Producer[PoolId, ConfirmedIndexed[CFMMPool], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, PoolId, ConfirmedIndexed[CFMMPool]](configs.producers.confirmedAmmPools)
      implicit0(producer3: Producer[OrderId, Unconfirmed[CFMMOrder], StreamF]) <-
        Resource.eval(Producer.dummy[InitF, StreamF, RunF, OrderId, Unconfirmed[CFMMOrder]](configs.producers.unconfirmedAmmOrders.topicId.value))
      implicit0(producer4: Producer[PoolId, Unconfirmed[CFMMPool], StreamF]) <-
        Resource.eval(Producer.dummy[InitF, StreamF, RunF, PoolId, Unconfirmed[CFMMPool]](configs.producers.unconfirmedAmmPools.topicId.value))
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(configs, blocker)
      implicit0(explorer: ErgoExplorerStreaming[StreamF, RunF]) = ErgoExplorerStreaming.make[StreamF, RunF]
      implicit0(node: ErgoNode[RunF]) <- Resource.eval(ErgoNode.make[InitF, RunF])
      implicit0(network: ErgoNetwork[RunF])         = ErgoNetwork.make[RunF]
      implicit0(ledger: LedgerStreaming[StreamF])   = LedgerStreaming.make[StreamF, RunF]
      implicit0(mempool: MempoolStreaming[StreamF]) = MempoolStreaming.make[StreamF, RunF]
      implicit0(cfmmRules: CFMMRules[RunF])         = CFMMRules.make[RunF]
      confirmedAmmOrderHandler             <- Resource.eval(CFMMOpsHandler.make[InitF, StreamF, RunF, Confirmed])
      unconfirmedAmmOrderHandler           <- Resource.eval(CFMMOpsHandler.make[InitF, StreamF, RunF, Unconfirmed])
      confirmedAmmPoolsHandler             <- Resource.eval(SettledCFMMPoolsHandler.make[InitF, StreamF, RunF])
      unconfirmedAmmPoolsHandler           <- Resource.eval(CFMMPoolsHandler.make[InitF, StreamF, RunF, Unconfirmed])
      implicit0(redis: Redis.Plain[RunF])  <- Redis.make[InitF, RunF](configs.redis)
      implicit0(cache: TrackerCache[RunF]) <- Resource.eval(TrackerCache.make[InitF, RunF])
      ledgerTracker  <- Resource.eval(LedgerTracker.make[InitF, StreamF, RunF](TrackerMode.Live, lift(confirmedAmmOrderHandler), confirmedAmmPoolsHandler))
      mempoolTracker <- Resource.eval(MempoolTracker.make[InitF, StreamF, RunF](unconfirmedAmmOrderHandler, unconfirmedAmmPoolsHandler))
    } yield (ledgerTracker, mempoolTracker, configs)
  // format: on

  private def makeBackend(
    configs: ConfigBundle,
    blocker: Blocker
  )(implicit wr: WithRun[RunF, InitF, ConfigBundle]): Resource[InitF, SttpBackend[RunF, Fs2Streams[RunF]]] =
    Resource
      .eval(wr.concurrentEffect)
      .flatMap(implicit ce => AsyncHttpClientFs2Backend.resource[RunF](blocker))
      .mapK(wr.runContextK(configs))
}
