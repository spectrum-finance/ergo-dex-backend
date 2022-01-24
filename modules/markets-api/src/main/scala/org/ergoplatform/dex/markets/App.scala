package org.ergoplatform.dex.markets

import cats.effect.{Blocker, Resource}
import cats.tagless.syntax.functorK._
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.db.{doobieLogging, PostgresTransactor}
import org.ergoplatform.dex.markets.api.v1.HttpServer
import org.ergoplatform.dex.markets.api.v1.services.{AmmStats, LqLocks}
import org.ergoplatform.dex.markets.configs.ConfigBundle
import org.ergoplatform.dex.markets.modules.PriceSolver.{CryptoPriceSolver, FiatPriceSolver}
import org.ergoplatform.dex.markets.repositories.{Locks, Pools}
import org.ergoplatform.dex.markets.services.{FiatRates, Markets}
import org.ergoplatform.ergo.ErgoNetworkStreaming
import org.http4s.server.Server
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
import zio.{ExitCode, URIO, ZEnv, ZIO}

object App extends EnvApp[AppContext] {

  implicit val serverOptions: Http4sServerOptions[RunF, RunF] = Http4sServerOptions.default[RunF, RunF]

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    init(args.headOption).use(_ => ZIO.never).orDie

  private def init(configPathOpt: Option[String]): Resource[InitF, Server] =
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
      implicit0(client: ErgoNetworkStreaming[StreamF, RunF]) = ErgoNetworkStreaming.make[StreamF, RunF]
      implicit0(pools: Pools[xa.DB])                   <- Resource.eval(Pools.make[InitF, xa.DB])
      implicit0(locks: Locks[xa.DB])                   <- Resource.eval(Locks.make[InitF, xa.DB])
      implicit0(markets: Markets[RunF])                <- Resource.eval(Markets.make[InitF, RunF, xa.DB])
      implicit0(rates: FiatRates[RunF])                <- Resource.eval(FiatRates.make[InitF, RunF])
      implicit0(cryptoSolver: CryptoPriceSolver[RunF]) <- Resource.eval(CryptoPriceSolver.make[InitF, RunF])
      implicit0(fiatSolver: FiatPriceSolver[RunF])     <- Resource.eval(FiatPriceSolver.make[InitF, RunF])
      implicit0(stats: AmmStats[RunF]) = AmmStats.make[RunF, xa.DB]
      implicit0(locks: LqLocks[RunF])  = LqLocks.make[RunF, xa.DB]
      server <- HttpServer.make[InitF, RunF](configs.http, runtime.platform.executor.asEC)
    } yield server

  private def makeBackend(
    ctx: AppContext,
    blocker: Blocker
  )(implicit wr: WithRun[RunF, InitF, AppContext]): Resource[InitF, SttpBackend[RunF, Fs2Streams[RunF]]] =
    Resource
      .eval(wr.concurrentEffect)
      .flatMap(implicit ce => AsyncHttpClientFs2Backend.resource[RunF](blocker))
      .mapK(wr.runContextK(ctx))
}
