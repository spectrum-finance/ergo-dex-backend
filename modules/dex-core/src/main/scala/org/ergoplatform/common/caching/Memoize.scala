package org.ergoplatform.common.caching

import cats.{Functor, Monad}
import cats.effect.Clock
import cats.effect.concurrent.Ref
import cats.instances.option._
import cats.syntax.option._
import cats.syntax.traverse._
import tofu.concurrent.MakeRef
import tofu.syntax.monadic._
import tofu.syntax.time.now.millis

import scala.concurrent.duration.FiniteDuration

trait Memoize[F[_], A] {

  /** Memoize `a` for a defined amount of time.
    */
  def memoize(a: A, ttl: FiniteDuration): F[Unit]

  /** Try read the value `A`.
    * @return None if the var is empty ot expired
    *         Some[A] otherwise
    */
  def read: F[Option[A]]
}

object Memoize {

  def make[I[_]: Functor, F[_]: Monad: Clock, A](implicit makeRef: MakeRef[I, F]): I[Memoize[F, A]] =
    makeRef.refOf(none[(A, Long)]).map(new Live(_))

  final class Live[F[_]: Monad: Clock, A](
    store: Ref[F, Option[(A, Long)]]
  ) extends Memoize[F, A] {

    def memoize(a: A, ttl: FiniteDuration): F[Unit] =
      for {
        ts <- millis
        expiresAt = ts + ttl.toMillis
        _ <- store.set(Some(a -> expiresAt))
      } yield ()

    def read: F[Option[A]] =
      for {
        memoized <- store.get
        res <- memoized.flatTraverse { case (a, ts0) =>
                 millis map (ts => if (ts < ts0) Some(a) else None)
               }
      } yield res
  }
}
