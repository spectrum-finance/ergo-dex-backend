package org.ergoplatform.dex.index.processes

import cats.{FlatMap, Foldable, Functor}
import org.ergoplatform.dex.index.repos.CFMMRepo
import org.ergoplatform.dex.index.streaming.CFMMConsumer
import tofu.streams.{Chunks, Evals}
import tofu.syntax.streams.evals._
import tofu.syntax.streams.chunks._
import tofu.syntax.monadic._
import tofu.syntax.logging._
import cats.syntax.foldable._
import org.ergoplatform.dex.domain.amm.{Deposit, Redeem, Swap}
import tofu.logging.Logging

trait Indexing[F[_]] {
  def run: F[Unit]
}

object Indexing {

  final class CFMMIndexing[
    S[_]: Evals[*[_], F]: Chunks[*[_], C],
    F[_]: FlatMap: Logging,
    C[_]: Functor: Foldable
  ](
    orders: CFMMConsumer[S, F],
    repo: CFMMRepo[F]
  ) extends Indexing[S] {

    def run: S[Unit] =
      orders.stream.chunks.evalMap { rs =>
        val orders = rs.map(r => r.message).toList
        val (swaps, others) = orders.partitionEither {
          case o: Swap => Left(o)
          case o       => Right(o)
        }
        val (deposits, redeems) = others.partitionEither {
          case o: Deposit => Left(o)
          case o: Redeem  => Right(o)
        }
        (repo.insertSwaps(swaps) >>= (i => info"[$i] orders indexed")) >>
        (repo.insertDeposits(deposits) >>= (i => info"[$i] orders indexed")) >>
        (repo.insertRedeems(redeems) >>= (i => info"[$i] orders indexed"))
      }
  }
}
