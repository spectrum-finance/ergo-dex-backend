package org.ergoplatform.common

import cats.data.ReaderT
import fs2.Stream
import tofu.WithRun
import tofu.lift.{IsoK, Unlift}
import tofu.logging.{Loggable, LoggableContext, Logs}
import zio.interop.catz._
import zio.{RIO, ZEnv}

abstract class EnvApp[C: Loggable] extends CatsApp {

  type InitF[+A]   = RIO[ZEnv, A]
  type RunF[A]     = ReaderT[InitF, C, A]
  type StreamF[+A] = Stream[RunF, A]

  implicit def logs: Logs[InitF, RunF]                = Logs.withContext[InitF, RunF]
  implicit def logsInit: Logs[InitF, InitF]           = Logs.sync[InitF, InitF]
  implicit def loggableContext: LoggableContext[RunF] = LoggableContext.of[RunF].instance[C]

  val wr: WithRun[RunF, InitF, C] = implicitly

  def isoKRunByContext(ctx: C): IsoK[RunF, InitF] = IsoK.byFunK(wr.runContextK(ctx))(wr.liftF)

  implicit val unlift: Unlift[InitF, RunF]  = wr
  implicit val isoK: IsoK[StreamF, StreamF] = IsoK.id[StreamF]
}
