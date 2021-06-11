package org.ergoplatform.dex.markets.processes

import cats.instances.list._
import cats.syntax.list._
import cats.syntax.traverse._
import cats.{Defer, Functor, Monad, MonoidK}
import derevo.derive
import org.ergoplatform.dex.markets.configs.IndexerConfig
import org.ergoplatform.dex.markets.modules.Fills
import org.ergoplatform.dex.markets.repositories.FillsRepo
import org.ergoplatform.dex.protocol.orderbook.{ContractTemplates, OrderContractFamily}
import org.ergoplatform.ergo.ErgoNetwork
import tofu.Catches
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Pace}
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

@derive(representableK)
trait MarketsIndexer[F[_]] {

  def run: F[Unit]
}

object MarketsIndexer {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: Pace: Defer: MonoidK: Catches: IndexerConfig.Has,
    G[_]: Monad,
    CT <: OrderContractFamily
  ](implicit
    logs: Logs[I, G],
    network: ErgoNetwork[G],
    tradesRepo: FillsRepo[G],
    trades: Fills[G, CT],
    templates: ContractTemplates[CT]
  ): I[MarketsIndexer[F]] =
    logs.forService[MarketsIndexer[F]].map { implicit l =>
      context[F].map { conf =>
        new Live[F, G, CT](conf): MarketsIndexer[F]
      }.embed
    }

  final class Live[
    F[_]: Monad: Evals[*[_], G]: Pace: Defer: MonoidK: Catches,
    G[_]: Monad: Logging,
    CT <: OrderContractFamily
  ](conf: IndexerConfig)(implicit
                         network: ErgoNetwork[G],
                         tradesRepo: FillsRepo[G],
                         trades: Fills[G, CT],
                         templates: ContractTemplates[CT]
  ) extends MarketsIndexer[F] {

    def run: F[Unit] =
      eval(tradesRepo.countTransactions).repeat
        .throttled(conf.scanInterval)
        .evalMap { count =>
          network
            .getTransactionsByInputScript(templates.ask.hash, count, conf.batchSize)
            .flatMap { txs =>
              txs.traverse(trades.extract).map(_.flatten) >>=
                (ts => info"Writing ${ts.size} trades" >> ts.toNel.fold(unit[G])(tradesRepo.insert))
            }
        }
  }
}
