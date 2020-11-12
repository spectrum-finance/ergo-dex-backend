package org.ergoplatform.dex.tracker

import cats.effect.ExitCode
import fs2._
import monix.eval.{Task, TaskApp}
import mouse.any._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.protocol.models.Transaction
import org.ergoplatform.dex.streaming.{Consumer, MakeKafkaConsumer, MakeKafkaProducer, Producer}
import org.ergoplatform.dex.tracker.context.AppContext
import org.ergoplatform.dex.tracker.processes.OrdersTracker
import org.ergoplatform.dex.tracker.streaming.StreamingBundle
import org.ergoplatform.dex.{OrderId, TxId}
import tofu.env.Env
import tofu.fs2Instances._
import tofu.lift.{IsoK, Unlift}
import tofu.logging.derivation.loggable.generate
import tofu.logging.{LoggableContext, Logs}
import tofu.syntax.unlift._

object App extends TaskApp {

  type InitF[+A]   = Task[A]
  type AppF[+A]    = Env[AppContext, A]
  type StreamF[+A] = Stream[AppF, A]

  implicit private def logs: Logs[InitF, AppF]                = Logs.withContext[InitF, AppF]
  implicit private def loggableContext: LoggableContext[AppF] = LoggableContext.of[AppF].instance[AppContext]

  def run(args: List[String]): Task[ExitCode] =
    for {
      (tracker, ctx) <- init(args.headOption)
      app = tracker.run.compile.drain
      _ <- app.run(ctx)
    } yield ExitCode.Success

  private def init(configPathOpt: Option[String]): InitF[(OrdersTracker[StreamF], AppContext)] =
    for {
      ctx <- AppContext.make[InitF](configPathOpt)
      implicit0(mc: MakeKafkaConsumer[AppF, TxId, Transaction]) <-
        Unlift[InitF, AppF].concurrentEffect
          .map(implicit ce => implicitly[MakeKafkaConsumer[AppF, TxId, Transaction]])
          .thrush(_.run(ctx))
      implicit0(mp: MakeKafkaProducer[AppF, OrderId, AnyOrder]) <-
        Unlift[InitF, AppF].concurrentEffect
          .map(implicit ce => implicitly[MakeKafkaProducer[AppF, OrderId, AnyOrder]])
          .thrush(_.run(ctx))
      implicit0(isoK: IsoK[StreamF, StreamF])           = IsoK.id[StreamF]
      consumer                                          = Consumer.make[StreamF, AppF, TxId, Transaction]
      producer                                          = Producer.make[StreamF, AppF, OrderId, AnyOrder]
      implicit0(bundle: StreamingBundle[StreamF, AppF]) = StreamingBundle(consumer, producer)
      implicit0(e: ErgoAddressEncoder)                  = ErgoAddressEncoder(ctx.protocolConfig.addressPrefix)
      tracker <- OrdersTracker.make[InitF, StreamF, AppF, Chunk]
    } yield tracker -> ctx
}
