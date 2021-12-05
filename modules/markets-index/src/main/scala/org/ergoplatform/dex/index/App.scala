package org.ergoplatform.dex.index

import cats.effect.{Blocker, Resource}
import fs2.Chunk
import fs2.kafka.serde._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.cache.{MakeRedisTransaction, Redis}
import org.ergoplatform.common.db.{doobieLogging, PostgresTransactor}
import org.ergoplatform.common.streaming.{Consumer, MakeKafkaConsumer, Producer}
import org.ergoplatform.dex.domain.amm.state.Confirmed
import org.ergoplatform.dex.domain.amm.{CFMMOrder, CFMMPool, EvaluatedCFMMOrder, OrderEvaluation, OrderId, PoolId}
import org.ergoplatform.dex.index.configs.ConfigBundle
import org.ergoplatform.dex.index.processes.{HistoryIndexing, PoolsIndexing}
import org.ergoplatform.dex.index.repos.RepoBundle
import org.ergoplatform.dex.index.streaming.{CFMMHistConsumer, CFMMPoolsConsumer}
import org.ergoplatform.dex.tracker.handlers.{CFMMHistoryHandler, CFMMPoolsHandler}
import org.ergoplatform.dex.tracker.processes.{TxTracker, UtxoTracker}
import org.ergoplatform.dex.tracker.processes.UtxoTracker.TrackerMode
import org.ergoplatform.dex.tracker.repositories.TrackerCache
import org.ergoplatform.ergo.ErgoNetworkStreaming
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import tofu.doobie.instances.implicits._
import tofu.WithRun
import tofu.concurrent.MakeRef
import tofu.doobie.log.EmbeddableLogHandler
import tofu.doobie.transactor.Txr
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.logging.Logs
import tofu.syntax.unlift._
import zio.interop.catz._
import zio.{ExitCode, URIO, ZEnv}

object App extends EnvApp[ConfigBundle] {

  implicit val makeRef: MakeRef[InitF, RunF]   = MakeRef.syncInstance
  implicit val mtx: MakeRedisTransaction[RunF] = MakeRedisTransaction.make[RunF]

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    init(args.headOption).use { case (utxoTracker, txTracker, pools, hist, ctx) =>
      val appF = fs2.Stream(utxoTracker.run, txTracker.run, pools.run, hist.run).parJoinUnbounded.compile.drain
      appF.run(ctx) as ExitCode.success
    }.orDie

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
      implicit0(mc: MakeKafkaConsumer[RunF, PoolId, Confirmed[CFMMPool]]) =
        MakeKafkaConsumer.make[InitF, RunF, PoolId, Confirmed[CFMMPool]]
      implicit0(poolCons: CFMMPoolsConsumer[StreamF, RunF]) =
        Consumer.make[StreamF, RunF, PoolId, Confirmed[CFMMPool]](configs.cfmmPoolsConsumer)
      implicit0(mkc: MakeKafkaConsumer[RunF, OrderId, EvaluatedCFMMOrder.Any]) =
        MakeKafkaConsumer.make[InitF, RunF, OrderId, EvaluatedCFMMOrder.Any]
      implicit0(orderCons: CFMMHistConsumer[StreamF, RunF]) =
        Consumer.make[StreamF, RunF, OrderId, EvaluatedCFMMOrder.Any](configs.cfmmHistoryConsumer)
      implicit0(orderProd: Producer[OrderId, EvaluatedCFMMOrder.Any, StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OrderId, EvaluatedCFMMOrder.Any](configs.cfmmHistoryProducer)
      implicit0(poolProd: Producer[PoolId, Confirmed[CFMMPool], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, PoolId, Confirmed[CFMMPool]](configs.cfmmPoolsProducer)
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(configs, blocker)
      implicit0(client: ErgoNetworkStreaming[StreamF, RunF]) = ErgoNetworkStreaming.make[StreamF, RunF]
      cfmmPoolsHandler                     <- Resource.eval(CFMMPoolsHandler.make[InitF, StreamF, RunF])
      cfmmHistoryHandler                   <- Resource.eval(CFMMHistoryHandler.make[InitF, StreamF, RunF])
      implicit0(redis: Redis.Plain[RunF])  <- Redis.make[InitF, RunF](configs.redis)
      implicit0(cache: TrackerCache[RunF]) <- Resource.eval(TrackerCache.make[InitF, RunF])
      utxoTracker <-
        Resource.eval(UtxoTracker.make[InitF, StreamF, RunF](TrackerMode.Historical, cfmmPoolsHandler))
      txTracker                           <- Resource.eval(TxTracker.make[InitF, StreamF, RunF](cfmmHistoryHandler))
      implicit0(repos: RepoBundle[xa.DB]) <- Resource.eval(RepoBundle.make[InitF, xa.DB])
      historyIndexer                      <- Resource.eval(HistoryIndexing.make[InitF, StreamF, RunF, xa.DB, Chunk])
      poolsIndexer                        <- Resource.eval(PoolsIndexing.make[InitF, StreamF, RunF, xa.DB, Chunk])
    } yield (utxoTracker, txTracker, poolsIndexer, historyIndexer, configs)

  private def makeBackend(
    configs: ConfigBundle,
    blocker: Blocker
  )(implicit wr: WithRun[RunF, InitF, ConfigBundle]): Resource[InitF, SttpBackend[RunF, Fs2Streams[RunF]]] =
    Resource
      .eval(wr.concurrentEffect)
      .flatMap(implicit ce => AsyncHttpClientFs2Backend.resource[RunF](blocker))
      .mapK(wr.runContextK(configs))
}
