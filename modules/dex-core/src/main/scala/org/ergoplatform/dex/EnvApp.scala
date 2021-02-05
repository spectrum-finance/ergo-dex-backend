package org.ergoplatform.dex

import fs2.Stream
import monix.eval.{Task, TaskApp}
import tofu.WithRun
import tofu.env.Env
import tofu.lift.IsoK
import tofu.logging.{Loggable, LoggableContext, Logs}

abstract class EnvApp[C: Loggable] extends TaskApp {

  type InitF[+A]   = Task[A]
  type RunF[+A]    = Env[C, A]
  type StreamF[+A] = Stream[RunF, A]

  implicit def logs: Logs[InitF, RunF]                = Logs.withContext[InitF, RunF]
  implicit def loggableContext: LoggableContext[RunF] = LoggableContext.of[RunF].instance[C]

  implicit val isoK: IsoK[StreamF, StreamF] = IsoK.id[StreamF]

  val wr: WithRun[RunF, InitF, C] = implicitly
}
