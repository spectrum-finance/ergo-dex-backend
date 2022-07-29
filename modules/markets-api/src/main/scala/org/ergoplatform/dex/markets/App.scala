package org.ergoplatform.dex.markets

import cats.effect.concurrent.Ref
import cats.effect.{Blocker, Resource}
import cats.tagless.syntax.functorK._
import fs2.kafka.serde._
import fs2.kafka.RecordDeserializer
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.cache.{Cache, MakeRedisTransaction, Redis}
import org.ergoplatform.common.db.{doobieLogging, PostgresTransactor}
import org.ergoplatform.common.http.cache.{CacheMiddleware, HttpCacheInvalidator, HttpResponseCaching}
import org.ergoplatform.common.http.cache.CacheMiddleware.CachingMiddleware
import org.ergoplatform.common.streaming.{Consumer, MakeKafkaConsumer}
import org.ergoplatform.dex.configs.ConsumerConfig
import org.ergoplatform.dex.index.streaming.BlocksConsumer
import org.ergoplatform.dex.markets.api.v1.HttpServer
import org.ergoplatform.dex.markets.api.v1.services.{AmmStats, LqLocks}
import org.ergoplatform.dex.markets.configs.ConfigBundle
import org.ergoplatform.dex.markets.modules.PriceSolver.{CryptoPriceSolver, FiatPriceSolver}
import org.ergoplatform.dex.markets.repositories.{Locks, Orders, Pools}
import org.ergoplatform.dex.markets.services.{FiatRates, Markets, TokenFetcher}
import org.ergoplatform.ergo.BlockId
import org.ergoplatform.ergo.domain.Block
import org.ergoplatform.ergo.modules.ErgoNetwork
import org.ergoplatform.ergo.services.explorer.ErgoExplorerStreaming
import org.ergoplatform.ergo.services.node.ErgoNode
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.tapir.server.http4s.Http4sServerOptions
import tofu.WithRun
import tofu.doobie.instances.implicits._
import tofu.doobie.log.EmbeddableLogHandler
import tofu.doobie.transactor.Txr
import tofu.fs2Instances._
import tofu.lift.{IsoK, Unlift}
import tofu.logging.Logs
import tofu.syntax.unlift._
import zio.interop.catz._
import zio.{ExitCode, URIO, ZEnv}
import cats.syntax.option._
import org.ergoplatform.dex.markets.processes.RatesProcess

object App extends EnvApp[AppContext] {

  implicit val serverOptions: Http4sServerOptions[RunF, RunF] = Http4sServerOptions.default[RunF, RunF]

  implicit val mtx: MakeRedisTransaction[RunF] = MakeRedisTransaction.make[RunF]

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    init(args.headOption).use { case (processes, ctx) =>
      val appF = fs2.Stream(processes: _*).parJoinUnbounded.compile.drain
      appF.run(ctx) as ExitCode.success
    }.orDie

  private def init(configPathOpt: Option[String]): Resource[InitF, (List[StreamF[Unit]], AppContext)] =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.eval(ConfigBundle.load[InitF](configPathOpt, blocker))
      ctx = AppContext.init(configs)
      trans <- PostgresTransactor.make("markets-api-pool", configs.db)
      implicit0(ul: Unlift[RunF, InitF])              = Unlift.byIso(IsoK.byFunK(wr.runContextK(ctx))(wr.liftF))
      implicit0(xa: Txr.Contextual[RunF, AppContext]) = Txr.contextual[RunF](trans)
      implicit0(elh: EmbeddableLogHandler[xa.DB]) <-
        Resource.eval(doobieLogging.makeEmbeddableHandler[InitF, RunF, xa.DB]("matcher-db-logging"))
      implicit0(logsDb: Logs[InitF, xa.DB]) = Logs.sync[InitF, xa.DB]
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(ctx, blocker)
      implicit0(client: ErgoExplorerStreaming[StreamF, RunF]) = ErgoExplorerStreaming.make[StreamF, RunF]
      implicit0(blocksCons: BlocksConsumer[StreamF, RunF])    = makeConsumer[BlockId, Block](configs.consumers.blocks)
      implicit0(pools: Pools[xa.DB])                      <- Resource.eval(Pools.make[InitF, xa.DB])
      implicit0(orders: Orders[xa.DB])                    <- Resource.eval(Orders.make[InitF, xa.DB])
      implicit0(locks: Locks[xa.DB])                      <- Resource.eval(Locks.make[InitF, xa.DB])
      implicit0(redis: Redis.Plain[RunF])                 <- Redis.make[InitF, RunF](configs.redis)
      implicit0(cache: Cache[RunF])                       <- Resource.eval(Cache.make[InitF, RunF])
      implicit0(httpRespCache: HttpResponseCaching[RunF]) <- Resource.eval(HttpResponseCaching.make[InitF, RunF])
      implicit0(httpCache: CachingMiddleware[RunF]) = CacheMiddleware.make[RunF]
      implicit0(markets: Markets[RunF])                <- Resource.eval(Markets.make[InitF, RunF, xa.DB])
      implicit0(cache: Ref[RunF, Option[BigDecimal]])  <- Resource.eval(Ref.in[InitF, RunF, Option[BigDecimal]](none))
      implicit0(rates: FiatRates[RunF])                <- Resource.eval(FiatRates.make[InitF, RunF])
      implicit0(ratesProcess: RatesProcess[StreamF])   <- Resource.eval(RatesProcess.make[InitF, RunF, StreamF])
      implicit0(cryptoSolver: CryptoPriceSolver[RunF]) <- Resource.eval(CryptoPriceSolver.make[InitF, RunF])
      implicit0(fiatSolver: FiatPriceSolver[RunF])     <- Resource.eval(FiatPriceSolver.make[InitF, RunF])
      implicit0(tokenFetcher: TokenFetcher[RunF])      <- Resource.eval(TokenFetcher.make[InitF, RunF])
      implicit0(node: ErgoNode[RunF])                  <- Resource.eval(ErgoNode.make[InitF, RunF])
      implicit0(network: ErgoNetwork[RunF]) = ErgoNetwork.make[RunF]
      implicit0(stats: AmmStats[RunF])      = AmmStats.make[RunF, xa.DB]
      implicit0(locks: LqLocks[RunF])       = LqLocks.make[RunF, xa.DB]

      invalidator     = HttpCacheInvalidator.make[StreamF, RunF, fs2.Chunk]
      invalidatorProc = invalidator.run
      serverProc      = HttpServer.make[InitF, RunF](configs.http, runtime.platform.executor.asEC, configs.request)
    } yield List(ratesProcess.run, invalidatorProc, serverProc.translate(wr.liftF).drain) -> ctx

  private def makeConsumer[K: RecordDeserializer[RunF, *], V: RecordDeserializer[RunF, *]](conf: ConsumerConfig) = {
    implicit val maker = MakeKafkaConsumer.make[InitF, RunF, K, V]
    Consumer.make[StreamF, RunF, K, V](conf)
  }

  private def makeBackend(
    ctx: AppContext,
    blocker: Blocker
  )(implicit wr: WithRun[RunF, InitF, AppContext]): Resource[InitF, SttpBackend[RunF, Fs2Streams[RunF]]] =
    Resource
      .eval(wr.concurrentEffect)
      .flatMap(implicit ce => AsyncHttpClientFs2Backend.resource[RunF](blocker))
      .mapK(wr.runContextK(ctx))

}
