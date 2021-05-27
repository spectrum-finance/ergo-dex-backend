package org.ergoplatform.common.streaming

import cats.data.Chain

/** A message associated with some offset that can be committed.
  */
trait Committable[K, V, O, F[_]] {

  def key: K

  def message: V

  def offset: O

  def commit: O => F[Unit]

  def batchCommit: Chain[O] => F[Unit]
}
