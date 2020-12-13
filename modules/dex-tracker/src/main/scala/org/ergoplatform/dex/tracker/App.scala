package org.ergoplatform.dex.tracker

import cats.effect.{Blocker, ExitCode, Resource}
import fs2._
import monix.eval.{Task, TaskApp}
import mouse.any._
import org.ergoplatform.dex.OrderId
import org.ergoplatform.dex.clients.StreamingErgoNetworkClient
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.streaming.{MakeKafkaProducer, Producer}
import org.ergoplatform.dex.tracker.configs.ConfigBundle
import org.ergoplatform.dex.tracker.processes.OrdersTracker
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import tofu.WithRun
import tofu.concurrent.MakeRef
import tofu.env.Env
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.logging.derivation.loggable.generate
import tofu.logging.{LoggableContext, Logs}
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.unlift._

object App extends TaskApp {

  type InitF[+A]   = Task[A]
  type RunF[+A]    = Env[ConfigBundle, A]
  type StreamF[+A] = Stream[RunF, A]

  implicit private def logs: Logs[InitF, RunF]                = Logs.withContext
  implicit private def makeRef: MakeRef[InitF, RunF]          = MakeRef.syncInstance
  implicit private def loggableContext: LoggableContext[RunF] = LoggableContext.of[RunF].instance[ConfigBundle]

  def run(args: List[String]): Task[ExitCode] =
    init(args.headOption).use { case (tracker, ctx) =>
      val appF = tracker.run.compile.drain
      appF.run(ctx) as ExitCode.Success
    }

  private def init(configPathOpt: Option[String]): Resource[InitF, (OrdersTracker[StreamF], ConfigBundle)] =
    for {
      blocker <- Blocker[InitF]
      configs <- Resource.liftF(ConfigBundle.load(configPathOpt))
      implicit0(mp: MakeKafkaProducer[RunF, OrderId, AnyOrder]) = MakeKafkaProducer.make[InitF, RunF, OrderId, AnyOrder]
      implicit0(isoK: IsoK[StreamF, StreamF])                   = IsoK.id[StreamF]
      implicit0(producer: Producer[OrderId, AnyOrder, StreamF]) = Producer.make[StreamF, RunF, OrderId, AnyOrder]
      implicit0(backend: SttpBackend[RunF, Fs2Streams[RunF]]) <- makeBackend(configs, blocker)
      implicit0(client: StreamingErgoNetworkClient[StreamF, RunF]) = StreamingErgoNetworkClient.make[StreamF, RunF]
      tracker <- Resource.liftF(OrdersTracker.make[InitF, StreamF, RunF, Chunk])
    } yield tracker -> configs

  private def makeBackend(
    configs: ConfigBundle,
    blocker: Blocker
  )(implicit wr: WithRun[RunF, InitF, ConfigBundle]): Resource[InitF, SttpBackend[RunF, Fs2Streams[RunF]]] =
    Resource
      .liftF(wr.concurrentEffect)
      .flatMap(implicit ce => AsyncHttpClientFs2Backend.resource[RunF](blocker))
      .mapK(wr.runContextK(configs))
}
