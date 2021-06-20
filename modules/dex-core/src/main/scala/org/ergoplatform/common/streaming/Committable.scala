package org.ergoplatform.common.streaming

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
