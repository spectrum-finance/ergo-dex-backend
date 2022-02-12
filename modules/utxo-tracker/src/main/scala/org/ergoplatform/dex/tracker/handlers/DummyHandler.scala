package org.ergoplatform.dex.tracker.handlers

import cats.{Functor, FunctorFilter, Monad}
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.streams.all._
import tofu.syntax.logging._
import tofu.syntax.monadic._

final class DummyHandler[
  F[_]: Monad: Evals[*[_], G]: FunctorFilter,
  G[_]: Monad: Logging
] {

  def handler: BoxHandler[F] =
    _.evalMap(o => debug"Handling $o")
}

object DummyHandler {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Monad
  ](tag: String)(implicit logs: Logs[I, G]): I[BoxHandler[F]] =
    logs.byName(tag).map(implicit l => new DummyHandler[F, G].handler)
}
