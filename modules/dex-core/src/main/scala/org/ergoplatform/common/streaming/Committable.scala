package org.ergoplatform.common.streaming

import cats.Functor
import cats.data.Chain

/** A message associated with some offset that can be committed.
  */
trait Committable[K, V, O, F[_]] {

  def key: K

  def message: V

  def offset: O

  def commit1: O => F[Unit]

  def commitBatch: Chain[O] => F[Unit]

  final def commit: F[Unit] = commit1(offset)
}

object Committable {

  implicit def functor[F[_], K, Offset]: Functor[Committable[K, *, Offset, F]] =
    new Functor[Committable[K, *, Offset, F]] {

      def map[A, B](fa: Committable[K, A, Offset, F])(f: A => B): Committable[K, B, Offset, F] =
        new Committable[K, B, Offset, F] {
          def key: K                                = fa.key
          def message: B                            = f(fa.message)
          def offset: Offset                        = fa.offset
          def commit1: Offset => F[Unit]            = fa.commit1
          def commitBatch: Chain[Offset] => F[Unit] = fa.commitBatch
        }
    }
}
