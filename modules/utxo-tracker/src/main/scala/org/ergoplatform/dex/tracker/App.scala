package org.ergoplatform.dex.tracker

import cats.effect.{Blocker, Resource}
import fs2.kafka.RecordDeserializer
import fs2.kafka.serde._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.cache.{MakeRedisTransaction, Redis}
import org.ergoplatform.common.streaming.{Consumer, Delayed, MakeKafkaConsumer, Producer}
import org.ergoplatform.dex.configs.ConsumerConfig
import org.ergoplatform.dex.domain.amm.{CFMMOrder, CFMMPool, OrderId, PoolId}
import org.ergoplatform.dex.tracker.configs.ConfigBundle
import org.ergoplatform.dex.tracker.handlers.{lift, CFMMOpsHandler, CFMMPoolsHandler, SettledCFMMPoolsHandler}
import org.ergoplatform.dex.tracker.processes.LedgerTracker.TrackerMode
import org.ergoplatform.dex.tracker.processes.{LedgerTracker, MempoolTracker}
import org.ergoplatform.dex.tracker.repositories.TrackerCache
import org.ergoplatform.dex.tracker.streaming.{MempoolConsumer, MempoolEvent, TransactionConsumer, TransactionEvent}
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
import MempoolEvent._
import TransactionEvent._

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
      implicit0(producer1: Producer[OrderId, Confirmed[CFMMOrder.AnyOrder], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OrderId, Confirmed[CFMMOrder.AnyOrder]](configs.producers.confirmedAmmOrders)
      implicit0(producer2: Producer[PoolId, ConfirmedIndexed[CFMMPool], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, PoolId, ConfirmedIndexed[CFMMPool]](configs.producers.confirmedAmmPools)
      implicit0(producer3: Producer[OrderId, Unconfirmed[CFMMOrder.AnyOrder], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OrderId, Unconfirmed[CFMMOrder.AnyOrder]](configs.producers.unconfirmedAmmOrders)
      implicit0(producer4: Producer[PoolId, Unconfirmed[CFMMPool], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, PoolId, Unconfirmed[CFMMPool]](configs.producers.unconfirmedAmmPools)
      implicit0(consumerMempool: MempoolConsumer[StreamF, RunF]) =
        makeConsumer[String, Option[MempoolEvent]](configs.mempoolTxConsumer)
      implicit0(consumerLedger: TransactionConsumer[StreamF, RunF]) =
        makeConsumer[String, Option[TransactionEvent]](configs.ledgerTxConsumer)
      implicit0(cfmmRules: CFMMRules[RunF])         = CFMMRules.make[RunF](configs.tokenId)
      confirmedAmmOrderHandler             <- Resource.eval(CFMMOpsHandler.make[InitF, StreamF, RunF, Confirmed](configs.tokenId))
      unconfirmedAmmOrderHandler           <- Resource.eval(CFMMOpsHandler.make[InitF, StreamF, RunF, Unconfirmed](configs.tokenId))
      confirmedAmmPoolsHandler             <- Resource.eval(SettledCFMMPoolsHandler.make[InitF, StreamF, RunF])
      unconfirmedAmmPoolsHandler           <- Resource.eval(CFMMPoolsHandler.make[InitF, StreamF, RunF, Unconfirmed])
      ledgerTracker  <- Resource.eval(LedgerTracker.make[InitF, StreamF, RunF](consumerLedger, lift(confirmedAmmOrderHandler), confirmedAmmPoolsHandler))
      mempoolTracker <- Resource.eval(MempoolTracker.make[InitF, StreamF, RunF](consumerMempool, unconfirmedAmmOrderHandler, unconfirmedAmmPoolsHandler)))
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

  private def makeConsumer[K: RecordDeserializer[RunF, *], V: RecordDeserializer[RunF, *]](conf: ConsumerConfig) = {
    implicit val maker = MakeKafkaConsumer.make[InitF, RunF, K, V]
    Consumer.make[StreamF, RunF, K, V](conf)
  }
}
