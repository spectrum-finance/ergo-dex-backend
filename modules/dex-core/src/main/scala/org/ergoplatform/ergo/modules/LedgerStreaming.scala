package org.ergoplatform.ergo.modules

import cats.Functor
import derevo.derive
import org.ergoplatform.ergo.domain.SettledOutput
import org.ergoplatform.ergo.services.explorer.ErgoExplorerStreaming
import tofu.higherKind.derived.representableK
import tofu.syntax.monadic._

@derive(representableK)
trait LedgerStreaming[F[_]] {

  /** Get a stream of unspent outputs at the given global offset.
    */
  def streamUnspentOutputs(boxGixOffset: Long, limit: Int): F[SettledOutput]

  /** Get a stream of unspent outputs at the given global offset.
    */
  def streamOutputs(boxGixOffset: Long, limit: Int): F[SettledOutput]
}

object LedgerStreaming {

  def make[F[_]: Functor, G[_]](implicit explorer: ErgoExplorerStreaming[F, G]): LedgerStreaming[F] =
    new Live(explorer)

  final class Live[F[_]: Functor, G[_]](explorer: ErgoExplorerStreaming[F, G]) extends LedgerStreaming[F] {

    def streamOutputs(boxGixOffset: Long, limit: Int): F[SettledOutput] =
      explorer.streamOutputs(boxGixOffset, limit).map(SettledOutput.fromExplorer)

    def streamUnspentOutputs(boxGixOffset: Long, limit: Int): F[SettledOutput] =
      explorer.streamUnspentOutputs(boxGixOffset, limit).map(SettledOutput.fromExplorer)
  }
}
