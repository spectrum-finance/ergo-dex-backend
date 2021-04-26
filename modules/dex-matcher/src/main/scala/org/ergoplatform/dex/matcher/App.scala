package org.ergoplatform.dex.matcher

import cats.effect.{ExitCode, Resource}
import fs2.Chunk
import monix.eval.Task
import org.ergoplatform.dex.domain.orderbook.Order.AnyOrder
import org.ergoplatform.dex.domain.orderbook.Trade.AnyTrade
import org.ergoplatform.dex.matcher.configs.ConfigBundle
import org.ergoplatform.dex.db.{PostgresTransactor, doobieLogging}
import org.ergoplatform.dex.matcher.processes.Matcher
import org.ergoplatform.dex.matcher.repositories.OrdersRepo
import org.ergoplatform.dex.matcher.services.{LimitOrderBook, OrderBook}
import org.ergoplatform.dex.matcher.streaming.StreamingBundle
import org.ergoplatform.dex.streaming.{Consumer, MakeKafkaConsumer, Producer}
import org.ergoplatform.dex.{EnvApp, OrderId, TradeId}
import tofu.doobie.instances.implicits._
import tofu.doobie.log.EmbeddableLogHandler
import tofu.doobie.transactor.Txr
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.logging.Logs

object App extends EnvApp[ConfigBundle] {

  def run(args: List[String]): Task[ExitCode] =
    resources(args.headOption).use { case (matcher, ctx) =>
      val appF = matcher.run.compile.drain
      appF.run(ctx) as ExitCode.Success
    }

  private def resources(configPathOpt: Option[String]): Resource[InitF, (Matcher[StreamF], ConfigBundle)] =
    for {
      configs <- Resource.liftF(ConfigBundle.load[InitF](configPathOpt))
      trans   <- PostgresTransactor.make("matcher-pool", configs.db)
      implicit0(xa: Txr.Contextual[RunF, ConfigBundle]) = Txr.contextual[RunF](trans)
      implicit0(elh: EmbeddableLogHandler[xa.DB]) <-
        Resource.liftF(doobieLogging.makeEmbeddableHandler[InitF, RunF, xa.DB]("matcher-db-logging"))
      implicit0(logsDb: Logs[InitF, xa.DB]) = Logs.sync[InitF, xa.DB]
      implicit0(isoKRun: IsoK[RunF, InitF]) = IsoK.byFunK(wr.runContextK(configs))(wr.liftF)
      implicit0(ordersRepo: OrdersRepo[xa.DB]) <- Resource.liftF(OrdersRepo.make[InitF, xa.DB])
      implicit0(orderBook: OrderBook[RunF])    <- Resource.liftF(LimitOrderBook.make[InitF, RunF, xa.DB])
      implicit0(mc: MakeKafkaConsumer[RunF, OrderId, AnyOrder])  = MakeKafkaConsumer.make[InitF, RunF, OrderId, AnyOrder]
      consumer                                                   = Consumer.make[StreamF, RunF, OrderId, AnyOrder]
      producer <- Producer.make[InitF, StreamF, RunF, TradeId, AnyTrade](configs.producer)
      implicit0(bundle: StreamingBundle[StreamF, RunF]) = StreamingBundle(consumer, producer)
      matcher <- Resource.liftF(Matcher.make[InitF, StreamF, RunF, Chunk])
    } yield matcher -> configs
}
