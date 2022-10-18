package org.ergoplatform.dex.index

import cats.effect.{Blocker, Resource}
import fs2.Chunk
import fs2.kafka.RecordDeserializer
import fs2.kafka.serde._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.cache.{MakeRedisTransaction, Redis}
import org.ergoplatform.common.db.{doobieLogging, PostgresTransactor}
import org.ergoplatform.common.streaming.{Consumer, MakeKafkaConsumer, Producer}
import org.ergoplatform.dex.configs.ConsumerConfig
import org.ergoplatform.dex.domain.amm.{CFMMPool, EvaluatedCFMMOrder, OrderId, PoolId}
import org.ergoplatform.dex.domain.locks.LiquidityLock
import org.ergoplatform.dex.domain.locks.types.LockId
import org.ergoplatform.dex.index.configs.ConfigBundle
import org.ergoplatform.dex.index.processes.{
  AnyOrdersHandler,
  BlockIndexing,
  HistoryIndexing,
  LiquidityProvidersIndexing,
  LocksIndexing,
  PoolsIndexing
}
import org.ergoplatform.dex.index.repositories.{LiquidityProvidersRepo, RepoBundle}
import org.ergoplatform.dex.index.streaming.{
  BlocksConsumer,
  CFMMHistConsumer,
  CFMMPoolsConsumer,
  LqLocksConsumer,
  TxnsConsumer
}
import org.ergoplatform.dex.tracker.handlers._
import org.ergoplatform.dex.tracker.processes.{BlockTracker, TxTracker}
import org.ergoplatform.dex.tracker.repositories.TrackerCache
import org.ergoplatform.ergo.{BlockId, TxId}
import org.ergoplatform.ergo.domain.{Block, ExtendedSettledTx}
import org.ergoplatform.ergo.modules.{ErgoNetwork, LedgerStreaming}
import org.ergoplatform.ergo.services.explorer.ErgoExplorerStreaming
import org.ergoplatform.ergo.services.node.ErgoNode
import org.ergoplatform.ergo.state.{Confirmed, ConfirmedIndexed}
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import tofu.WithRun
import tofu.concurrent.MakeRef
import tofu.doobie.instances.implicits._
import tofu.doobie.log.EmbeddableLogHandler
import tofu.doobie.transactor.Txr
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.logging.Logs
import tofu.syntax.unlift._
import zio.interop.catz._
import zio.{ExitCode, URIO, ZEnv}
import OrdersHandler._

object App extends EnvApp[ConfigBundle] {

  implicit val makeRef: MakeRef[InitF, RunF]   = MakeRef.syncInstance
  implicit val mtx: MakeRedisTransaction[RunF] = MakeRedisTransaction.make[RunF]

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    init(args.headOption).use { case (processes, ctx) =>
      val appF = fs2.Stream(processes: _*).parJoinUnbounded.compile.drain
      appF.run(ctx) as ExitCode.success
    }.orDie

  // format:off
  private def init(configPathOpt: Option[String]) =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load[InitF](configPathOpt, blocker))
      trans   <- PostgresTransactor.make("matcher-pool", configs.db)
      implicit0(xa: Txr.Contextual[RunF, ConfigBundle]) = Txr.contextual[RunF](trans)
      implicit0(elh: EmbeddableLogHandler[xa.DB]) <-
        Resource.eval(doobieLogging.makeEmbeddableHandler[InitF, RunF, xa.DB]("indexer-db-logging"))
      implicit0(e: ErgoAddressEncoder)      = configs.protocol.networkType.addressEncoder
      implicit0(isoKRun: IsoK[RunF, InitF]) = isoKRunByContext(configs)
      implicit0(logsDb: Logs[InitF, xa.DB]) = Logs.sync[InitF, xa.DB]
      implicit0(txnsConsumer: TxnsConsumer[StreamF, RunF]) =
        makeConsumer[TxId, ExtendedSettledTx](configs.consumers.txns)
      implicit0(poolCons: CFMMPoolsConsumer[StreamF, RunF]) =
        makeConsumer[PoolId, ConfirmedIndexed[CFMMPool]](configs.consumers.cfmmPools)
      implicit0(ammHistCons: CFMMHistConsumer[StreamF, RunF]) =
        makeConsumer[OrderId, Option[EvaluatedCFMMOrder.Any]](configs.consumers.cfmmHistory)
      implicit0(locksCons: LqLocksConsumer[StreamF, RunF]) =
        makeConsumer[LockId, Confirmed[LiquidityLock]](configs.consumers.lqLocks)
      implicit0(blocksCons: BlocksConsumer[StreamF, RunF]) =
        makeConsumer[BlockId, Block](configs.consumers.blocks)
      implicit0(orderProd: Producer[OrderId, EvaluatedCFMMOrder.Any, StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OrderId, EvaluatedCFMMOrder.Any](configs.producers.cfmmHistory)
      implicit0(poolProd: Producer[PoolId, ConfirmedIndexed[CFMMPool], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, PoolId, ConfirmedIndexed[CFMMPool]](configs.producers.cfmmPools)
      implicit0(lockProd: Producer[LockId, Confirmed[LiquidityLock], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, LockId, Confirmed[LiquidityLock]](configs.producers.lqLocks)
      implicit0(blockProd: Producer[BlockId, Block, StreamF]) <-
        Producer.make[InitF, StreamF, RunF, BlockId, Block](configs.producers.blocks)
      implicit0(txProd: Producer[TxId, ExtendedSettledTx, StreamF]) <-
        Producer.make[InitF, StreamF, RunF, TxId, ExtendedSettledTx](configs.producers.txns)
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(configs, blocker)
      implicit0(explorer: ErgoExplorerStreaming[StreamF, RunF]) = ErgoExplorerStreaming.make[StreamF, RunF]
      implicit0(node: ErgoNode[RunF]) <- Resource.eval(ErgoNode.make[InitF, RunF])
      implicit0(network: ErgoNetwork[RunF])       = ErgoNetwork.make[RunF]
      implicit0(ledger: LedgerStreaming[StreamF]) = LedgerStreaming.make[StreamF, RunF]
      cfmmPoolsHandler <-
        Resource.eval(SettledCFMMPoolsHandler.make[InitF, StreamF, RunF]).map(liftSettledOutputs[StreamF])
      lqLocksHandler                                <- Resource.eval(LiquidityLocksHandler.make[InitF, StreamF, RunF]).map(liftOutputs[StreamF])
      cfmmHistoryHandler                            <- Resource.eval(CFMMHistoryHandler.make[InitF, StreamF, RunF]).map(liftSettledTx[StreamF])
      blockHandler                                  <- Resource.eval(BlockHistoryHandler.make[InitF, StreamF, RunF])
      txHandler                                     <- Resource.eval(ExtendedSettledTxHandler.make[InitF, StreamF, RunF])
      implicit0(redis: Redis.Plain[RunF])           <- Redis.make[InitF, RunF](configs.redis)
      implicit0(cache: TrackerCache[RunF])          <- Resource.eval(TrackerCache.make[InitF, RunF])
      blockTracker                                  <- Resource.eval(BlockTracker.make[InitF, StreamF, RunF](blockHandler))
      implicit0(repo: LiquidityProvidersRepo[RunF]) <- Resource.eval(LiquidityProvidersRepo.make[InitF, RunF, xa.DB])
//      stateIndexing <-
//        Resource.eval(LiquidityProvidersIndexing.make[InitF, StreamF, RunF, Chunk](configs.stateIndexerConfig))
      txTracker <-
        Resource.eval(
          TxTracker.make[InitF, StreamF, RunF](
            List(cfmmHistoryHandler, cfmmPoolsHandler, lqLocksHandler, txHandler)
          )
        )
      implicit0(repos: RepoBundle[xa.DB]) <- Resource.eval(RepoBundle.make[InitF, xa.DB])
      implicit0(handlers: List[AnyOrdersHandler[xa.DB]]) = AnyOrdersHandler.makeOrdersHandlers[xa.DB]
      historyIndexer <- Resource.eval(HistoryIndexing.make[InitF, StreamF, RunF, xa.DB, Chunk])
      poolsIndexer   <- Resource.eval(PoolsIndexing.make[InitF, StreamF, RunF, xa.DB, Chunk])
      locksIndexer   <- Resource.eval(LocksIndexing.make[InitF, StreamF, RunF, xa.DB, Chunk])
      blocksIndexer  <- Resource.eval(BlockIndexing.make[InitF, StreamF, RunF, xa.DB, Chunk])
      processes =
        txTracker.run :: blockTracker.run :: blocksIndexer.run :: poolsIndexer.run :: historyIndexer.run :: locksIndexer.run :: Nil
    } yield (processes, configs)
  // format:on

  private def makeConsumer[K: RecordDeserializer[RunF, *], V: RecordDeserializer[RunF, *]](conf: ConsumerConfig) = {
    implicit val maker = MakeKafkaConsumer.make[InitF, RunF, K, V]
    Consumer.make[StreamF, RunF, K, V](conf)
  }

  private def makeBackend(
    configs: ConfigBundle,
    blocker: Blocker
  )(implicit wr: WithRun[RunF, InitF, ConfigBundle]): Resource[InitF, SttpBackend[RunF, Fs2Streams[RunF]]] =
    Resource
      .eval(wr.concurrentEffect)
      .flatMap(implicit ce => AsyncHttpClientFs2Backend.resource[RunF](blocker))
      .mapK(wr.runContextK(configs))
}
