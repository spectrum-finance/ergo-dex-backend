package org.ergoplatform.ergo.modules

import cats.Functor
import derevo.derive
import org.ergoplatform.ergo.domain.{Block, SettledOutput, SettledTransaction}
import org.ergoplatform.ergo.services.explorer.ErgoExplorerStreaming
import tofu.higherKind.derived.representableK
import tofu.syntax.monadic._

@derive(representableK)
trait LedgerStreaming[F[_]] {

  /** Get a stream of unspent outputs at the given global offset.
    */
  def streamUnspentOutputs(gOffset: Long, limit: Int): F[SettledOutput]

  /** Get a stream of unspent outputs at the given global offset.
    */
  def streamOutputs(gOffset: Long, limit: Int): F[SettledOutput]

  /** Get a stream of transactions at the given global offset.
   */
  def streamTxs(gOffset: Long, limit: Int): F[SettledTransaction]

  /** Get a stream of blocks at the given offset(height).
    */
  def streamBlocks(gOffset: Long, limit: Int): F[Block]
}

object LedgerStreaming {

  def make[F[_]: Functor, G[_]](implicit explorer: ErgoExplorerStreaming[F, G]): LedgerStreaming[F] =
    new Live(explorer)

  final class Live[F[_]: Functor, G[_]](explorer: ErgoExplorerStreaming[F, G]) extends LedgerStreaming[F] {

    def streamOutputs(gOffset: Long, limit: Int): F[SettledOutput] =
      explorer.streamOutputs(gOffset, limit).map(SettledOutput.fromExplorer)

    def streamUnspentOutputs(gOffset: Long, limit: Int): F[SettledOutput] =
      explorer.streamUnspentOutputs(gOffset, limit).map(SettledOutput.fromExplorer)

    def streamTxs(gOffset: Long, limit: Int): F[SettledTransaction] =
      explorer.streamTransactions(gOffset, limit).map(SettledTransaction.fromExplorer)

    def streamBlocks(gOffset: Long, limit: Int): F[Block] =
      explorer.streamBlocks(gOffset, limit).map(Block.fromExplorer)
  }
}
