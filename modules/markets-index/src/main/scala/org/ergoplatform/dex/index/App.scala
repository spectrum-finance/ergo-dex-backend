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
import org.ergoplatform.dex.domain.amm.{CFMMOrder, CFMMPool, OrderId, PoolId}
import org.ergoplatform.dex.index.configs.ConfigBundle
import org.ergoplatform.dex.index.processes.Indexing
import org.ergoplatform.dex.index.repos.RepoBundle
import org.ergoplatform.dex.index.streaming.CFMMConsumer
import org.ergoplatform.dex.tracker.handlers.{CFMMOpsHandler, CFMMPoolsHandler}
import org.ergoplatform.dex.tracker.processes.UtxoTracker
import org.ergoplatform.dex.tracker.processes.UtxoTracker.TrackerMode
import org.ergoplatform.dex.tracker.repositories.TrackerCache
import org.ergoplatform.dex.tracker.validation.amm.CFMMRules
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
    init(args.headOption).use { case (tracker, ctx) =>
      val appF = tracker.run.compile.drain
      appF.run(ctx) as ExitCode.success
    }.orDie

  private def init(configPathOpt: Option[String]): Resource[InitF, (UtxoTracker[StreamF], ConfigBundle)] =
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
      implicit0(mkc: MakeKafkaConsumer[RunF, OrderId, CFMMOrder]) =
        MakeKafkaConsumer.make[InitF, RunF, OrderId, CFMMOrder]
      implicit0(consumer: CFMMConsumer[StreamF, RunF]) =
        Consumer.make[StreamF, RunF, OrderId, CFMMOrder](configs.cfmmOrdersConsumer)
      implicit0(producer1: Producer[OrderId, CFMMOrder, StreamF]) <-
        Producer.make[InitF, StreamF, RunF, OrderId, CFMMOrder](configs.cfmmOrdersProducer)
      implicit0(producer2: Producer[PoolId, Confirmed[CFMMPool], StreamF]) <-
        Producer.make[InitF, StreamF, RunF, PoolId, Confirmed[CFMMPool]](configs.cfmmPoolsProducer)
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(configs, blocker)
      implicit0(client: ErgoNetworkStreaming[StreamF, RunF]) = ErgoNetworkStreaming.make[StreamF, RunF]
      implicit0(cfmmRules: CFMMRules[RunF])                  = CFMMRules.make[RunF]
      t2tCfmmHandler                       <- Resource.eval(CFMMOpsHandler.make[InitF, StreamF, RunF])
      cfmmPoolsHandler                     <- Resource.eval(CFMMPoolsHandler.make[InitF, StreamF, RunF])
      implicit0(redis: Redis.Plain[RunF])  <- Redis.make[InitF, RunF](configs.redis)
      implicit0(cache: TrackerCache[RunF]) <- Resource.eval(TrackerCache.make[InitF, RunF])
      tracker <-
        Resource.eval(UtxoTracker.make[InitF, StreamF, RunF](TrackerMode.Historical, cfmmPoolsHandler))
      implicit0(repos: RepoBundle[xa.DB]) <- Resource.eval(RepoBundle.make[InitF, xa.DB])
      indexer                             <- Resource.eval(Indexing.make[InitF, StreamF, RunF, xa.DB, Chunk])
    } yield tracker -> configs

  private def makeBackend(
    configs: ConfigBundle,
    blocker: Blocker
  )(implicit wr: WithRun[RunF, InitF, ConfigBundle]): Resource[InitF, SttpBackend[RunF, Fs2Streams[RunF]]] =
    Resource
      .eval(wr.concurrentEffect)
      .flatMap(implicit ce => AsyncHttpClientFs2Backend.resource[RunF](blocker))
      .mapK(wr.runContextK(configs))
}
