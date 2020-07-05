package org.ergoplatform.dex.executor.services

import cats.Monad
import cats.data.NonEmptyList
import org.ergoplatform.dex.domain.models.Match.AnyMatch
import org.ergoplatform.dex.domain.syntax.boxId._
import org.ergoplatform.dex.domain.syntax.match_._
import org.ergoplatform.{ErgoBoxCandidate, ErgoLikeTransaction, Input}
import sigmastate.interpreter.ProverResult
import tofu.syntax.monadic._

abstract class TransactionService[F[_]] {

  /** Assembly Ergo transaction from a given `match`.
    */
  def makeTx(anyMatch: AnyMatch): F[ErgoLikeTransaction]
}

object TransactionService {

  final private class Live[F[_]: Monad] extends TransactionService[F] {

    def makeTx(anyMatch: AnyMatch): F[ErgoLikeTransaction] = {
      val in  = inputs(anyMatch).toList.toVector
      val out = outputs(anyMatch).toList.toVector
      ErgoLikeTransaction(in, out).pure
    }
  }

  private[services] def inputs(anyMatch: AnyMatch): NonEmptyList[Input] =
    anyMatch.allOrders.map { ord =>
      Input(ord.meta.boxId.toErgo, ProverResult.empty)
    }

  private[services] def outputs(anyMatch: AnyMatch): NonEmptyList[ErgoBoxCandidate] = ???
}
