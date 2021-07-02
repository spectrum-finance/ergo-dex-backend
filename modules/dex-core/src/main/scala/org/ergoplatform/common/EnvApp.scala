package org.ergoplatform.common

import fs2.Stream
import monix.eval.{Task, TaskApp}
import tofu.WithRun
import tofu.env.Env
import tofu.lift.{IsoK, Unlift}
import tofu.logging.{Loggable, LoggableContext, Logs}

abstract class EnvApp[C: Loggable] extends TaskApp {

  type InitF[+A]   = Task[A]
  type RunF[+A]    = Env[C, A]
  type StreamF[+A] = Stream[RunF, A]

  implicit def logs: Logs[InitF, RunF]                = Logs.withContext[InitF, RunF]
  implicit def logsInit: Logs[InitF, InitF]           = Logs.sync[InitF, InitF]
  implicit def loggableContext: LoggableContext[RunF] = LoggableContext.of[RunF].instance[C]

  val wr: WithRun[RunF, InitF, C] = implicitly

  implicit val unlift: Unlift[InitF, RunF]  = wr
  implicit val isoK: IsoK[StreamF, StreamF] = IsoK.id[StreamF]
}
