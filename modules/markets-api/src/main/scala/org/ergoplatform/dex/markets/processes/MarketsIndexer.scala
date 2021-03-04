package org.ergoplatform.dex.markets.processes

import cats.{Defer, FunctorFilter, Monad, MonoidK}
import cats.instances.list._
import cats.syntax.traverse._
import cats.syntax.list._
import derevo.derive
import org.ergoplatform.dex.clients.ErgoNetworkClient
import org.ergoplatform.dex.markets.configs.IndexerConfig
import org.ergoplatform.dex.markets.modules.Trades
import org.ergoplatform.dex.markets.repositories.TradesRepo
import org.ergoplatform.dex.protocol.ScriptTemplates
import tofu.Catches
import tofu.higherKind.derived.representableK
import tofu.logging.Logging
import tofu.streams.{Evals, Pace}
import tofu.syntax.streams.all._
import tofu.syntax.monadic._
import tofu.syntax.logging._

@derive(representableK)
trait MarketsIndexer[F[_]] {

  def run: F[Unit]
}

object MarketsIndexer {

  final class Live[
    F[_]: Monad: Evals[*[_], G]: Pace: FunctorFilter: Defer: MonoidK: Catches,
    G[_]: Monad: Logging
  ](conf: IndexerConfig, templates: ScriptTemplates)(implicit network: ErgoNetworkClient[G], tradesRepo: TradesRepo[G], trades: Trades[G]) extends MarketsIndexer[F] {

    def run: F[Unit] =
      eval(tradesRepo.count).repeat
        .throttled(conf.scanInterval)
        .evalMap { count =>
          network.getTransactionsByInputScript(templates.limitOrderAsk, count, conf.batchSize)
            .flatMap { txs =>
              txs.traverse(trades.extract).map(_.flatten) >>=
                (ts => info"Writing ${ts.size} trades" >> ts.toNel.fold(unit[G])(tradesRepo.insert))
            }
        }
  }
}
