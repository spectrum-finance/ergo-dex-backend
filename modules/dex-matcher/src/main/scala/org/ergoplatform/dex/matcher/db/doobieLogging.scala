package org.ergoplatform.dex.matcher.db

import cats.Functor
import cats.syntax.functor._
import doobie.util.log._
import tofu.doobie.log.{EmbeddableLogHandler, LogHandlerF}
import tofu.lift.{Lift, UnliftIO}
import tofu.logging.{Logging, Logs}
import tofu.syntax.lift._

object doobieLogging {

  def makeEmbeddableHandler[
    I[_]: Functor,
    F[_]: Functor: UnliftIO,
    D[_]: Lift[F, *[_]]
  ](name: String)(implicit logs: Logs[I, F]): I[EmbeddableLogHandler[D]] =
    logs.byName(name).map { implicit log =>
      val lhf = LogHandlerF(logDoobieEvent)
      EmbeddableLogHandler.async(lhf).lift[D]
    }

  private def logDoobieEvent[F[_]](implicit log: Logging[F]): LogEvent => F[Unit] = {
    case Success(s, a, e1, e2) =>
      log.trace(
        s"""Successful Statement Execution:
           |
           |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
           |
           | arguments = [${a.mkString(", ")}]
           |   elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (${(e1 + e2).toMillis} ms total)
         """.stripMargin
      )

    case ProcessingFailure(s, a, e1, e2, t) =>
      log.error(
        s"""Failed Resultset Processing:
           |
           |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
           |
           | arguments = [${a.mkString(", ")}]
           |   elapsed = ${e1.toMillis} ms exec + ${e2.toMillis} ms processing (failed) (${(e1 + e2).toMillis} ms total)
           |   failure = ${Option(t.getMessage).getOrElse("")}
         """.stripMargin
      )

    case ExecFailure(s, a, e1, t) =>
      log.error(
        s"""Failed Statement Execution:
           |
           |  ${s.linesIterator.dropWhile(_.trim.isEmpty).mkString("\n  ")}
           |
           | arguments = [${a.mkString(", ")}]
           |   elapsed = ${e1.toMillis} ms exec (failed)
           |   failure = ${Option(t.getMessage).getOrElse("")}
         """.stripMargin
      )
  }
}