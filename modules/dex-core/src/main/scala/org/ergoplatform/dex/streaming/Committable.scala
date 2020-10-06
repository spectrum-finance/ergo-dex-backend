package org.ergoplatform.dex.streaming

import cats.data.Chain

/** A message associated with some offset that can be committed.
  */
trait Committable[A, O, F[_]] {

  def message: A

  def offset: O

  def commit: O => F[Unit]

  def batchCommit: Chain[O] => F[Unit]
}
