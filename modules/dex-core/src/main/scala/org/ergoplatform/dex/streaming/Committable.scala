package org.ergoplatform.dex.streaming

import cats.data.Chain

/** A message associated with some offset that can be committed.
  */
trait Committable[K, V, +O, F[_]] {

  def key: K

  def message: V

  def offset: O

  def commit[O1 >: O]: O1 => F[Unit]

  def batchCommit[O1 >: O]: Chain[O1] => F[Unit]
}
