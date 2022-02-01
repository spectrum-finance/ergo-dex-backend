package org.ergoplatform.ergo.modules

import org.ergoplatform.ergo.domain.SettledOutput

trait NetworkStreaming[F[_]] {

  /** Get a stream of unspent outputs at the given global offset.
   */
  def streamUnspentOutputs(boxGixOffset: Long, limit: Int): F[SettledOutput]

  /** Get a stream of unspent outputs at the given global offset.
   */
  def streamOutputs(boxGixOffset: Long, limit: Int): F[SettledOutput]
}
