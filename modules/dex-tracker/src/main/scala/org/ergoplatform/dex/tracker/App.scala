package org.ergoplatform.dex.tracker

import cats.effect.ExitCode
import fs2._
import monix.eval.{Task, TaskApp}
import mouse.any._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.protocol.models.Transaction
import org.ergoplatform.dex.streaming.{Consumer, MakeKafkaConsumer, MakeKafkaProducer, Producer}
import org.ergoplatform.dex.tracker.configs.ConfigBundle
import org.ergoplatform.dex.tracker.processes.OrdersTracker
import org.ergoplatform.dex.tracker.streaming.StreamingBundle
import org.ergoplatform.dex.{OrderId, TxId}
import tofu.env.Env
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.logging.derivation.loggable.generate
import tofu.logging.{LoggableContext, Logs}
import tofu.syntax.embed._

object App extends TaskApp {

  type InitF[+A]   = Task[A]
  type AppF[+A]    = Env[ConfigBundle, A]
  type StreamF[+A] = Stream[AppF, A]

  implicit private def logs: Logs[InitF, AppF]                = Logs.withContext[InitF, AppF]
  implicit private def loggableContext: LoggableContext[AppF] = LoggableContext.of[AppF].instance[ConfigBundle]

  def run(args: List[String]): Task[ExitCode] =
    for {
      (tracker, ctx) <- init(args.headOption)
      appF = tracker.run.compile.drain
      _ <- appF.run(ctx)
    } yield ExitCode.Success

  private def init(configPathOpt: Option[String]): InitF[(OrdersTracker[StreamF], ConfigBundle)] =
    for {
      configs <- ConfigBundle.load(configPathOpt)
      implicit0(mc: MakeKafkaConsumer[AppF, TxId, Transaction]) = MakeKafkaConsumer.make[InitF, AppF, TxId, Transaction]
      implicit0(mp: MakeKafkaProducer[AppF, OrderId, AnyOrder]) = MakeKafkaProducer.make[InitF, AppF, OrderId, AnyOrder]
      implicit0(isoK: IsoK[StreamF, StreamF])                   = IsoK.id[StreamF]
      consumer                                                  = Consumer.make[StreamF, AppF, TxId, Transaction]
      producer                                                  = Producer.make[StreamF, AppF, OrderId, AnyOrder]
      implicit0(bundle: StreamingBundle[StreamF, AppF])         = StreamingBundle(consumer, producer)
      implicit0(e: ErgoAddressEncoder)                          = ErgoAddressEncoder(configs.protocol.addressPrefix)
      tracker <- OrdersTracker.make[InitF, StreamF, AppF, Chunk]
    } yield tracker -> configs
}
