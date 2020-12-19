package org.ergoplatform.dex.matcher

import cats.effect.{ExitCode, Resource}
import fs2.{Chunk, Stream}
import monix.eval.{Task, TaskApp}
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.matcher.configs.ConfigBundle
import org.ergoplatform.dex.matcher.db.doobieLogging
import org.ergoplatform.dex.matcher.modules.PostgresTransactor
import org.ergoplatform.dex.matcher.processes.Matcher
import org.ergoplatform.dex.matcher.repositories.OrdersRepo
import org.ergoplatform.dex.matcher.services.{LimitOrderBook, OrderBook}
import org.ergoplatform.dex.matcher.streaming.StreamingBundle
import org.ergoplatform.dex.streaming.{Consumer, MakeKafkaConsumer, MakeKafkaProducer, Producer}
import org.ergoplatform.dex.{OrderId, TradeId}
import tofu.doobie.instances.implicits._
import tofu.doobie.log.EmbeddableLogHandler
import tofu.doobie.transactor.Txr
import tofu.env.Env
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.logging.{LoggableContext, Logs}

object App extends TaskApp {

  type InitF[+A]   = Task[A]
  type AppF[+A]    = Env[ConfigBundle, A]
  type StreamF[+A] = Stream[AppF, A]

  implicit private def logs: Logs[InitF, AppF]                = Logs.withContext[InitF, AppF]
  implicit private def loggableContext: LoggableContext[AppF] = LoggableContext.of[AppF].instance[ConfigBundle]

  def run(args: List[String]): Task[ExitCode] =
    resources(args.headOption).use { case (matcher, ctx) =>
      val appF = matcher.run.compile.drain
      appF.run(ctx) as ExitCode.Success
    }

  private def resources(configPathOpt: Option[String]): Resource[InitF, (Matcher[StreamF], ConfigBundle)] =
    for {
      configs <- Resource.liftF(ConfigBundle.load[InitF](configPathOpt))
      trans   <- PostgresTransactor.make("matcher-pool", configs.db)
      implicit0(xa: Txr.Contextual[AppF, ConfigBundle]) = Txr.contextual[AppF](trans)
      implicit0(elh: EmbeddableLogHandler[xa.DB]) <-
        Resource.liftF(doobieLogging.makeEmbeddableHandler[InitF, AppF, xa.DB]("matcher-db-logging"))
      implicit0(ordersRepo: OrdersRepo[xa.DB]) = OrdersRepo.make[xa.DB]
      implicit0(orderBook: OrderBook[AppF]) <- Resource.liftF(LimitOrderBook.make[InitF, AppF, xa.DB])
      implicit0(mc: MakeKafkaConsumer[AppF, OrderId, AnyOrder]) = MakeKafkaConsumer.make[InitF, AppF, OrderId, AnyOrder]
      implicit0(mp: MakeKafkaProducer[AppF, TradeId, AnyTrade]) = MakeKafkaProducer.make[AppF, TradeId, AnyTrade]
      implicit0(isoK: IsoK[StreamF, StreamF])                   = IsoK.id[StreamF]
      consumer                                                  = Consumer.make[StreamF, AppF, OrderId, AnyOrder]
      producer                                                  = Producer.make[StreamF, AppF, TradeId, AnyTrade]
      implicit0(bundle: StreamingBundle[StreamF, AppF])         = StreamingBundle(consumer, producer)
      matcher <- Resource.liftF(Matcher.make[InitF, StreamF, AppF, Chunk])
    } yield matcher -> configs
}
