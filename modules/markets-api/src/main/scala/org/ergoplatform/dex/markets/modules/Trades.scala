package org.ergoplatform.dex.markets.modules

import cats.Applicative
import org.ergoplatform.dex.clients.explorer.models.Transaction
import org.ergoplatform.dex.markets.models.{Side, Trade}
import org.ergoplatform.dex.protocol.{constants, ContractType, OrderParams, OrderScripts}
import tofu.syntax.monadic._

trait Trades[F[_], CT <: ContractType] {

  def extract(tx: Transaction): F[List[Trade]]
}

object Trades {

  implicit def instance[F[_]: Applicative, CT <: ContractType](implicit scripts: OrderScripts[CT]): Trades[F, CT] =
    new Live[F, CT]

  final class Live[F[_]: Applicative, CT <: ContractType](implicit scripts: OrderScripts[CT]) extends Trades[F, CT] {

    def extract(tx: Transaction): F[List[Trade]] = {
      val asksIn     = tx.inputs.filter(in => scripts.isAsk(in.ergoTree))
      val asksErased = asksIn.flatMap(scripts.parseAsk)
      val bidsIn     = tx.inputs.filter(in => scripts.isBid(in.ergoTree))
      val bidsErased = bidsIn.flatMap(scripts.parseBid)
      (collectSellTrades(tx, asksErased) ++ collectBuyTrades(tx, bidsErased)).pure
    }

    private def collectSellTrades(tx: Transaction, asks: List[OrderParams]): List[Trade] =
      asks.foldLeft(List.empty[Trade]) { case (acc, params) =>
        val trades = tx.outputs
          .find(out => out.ergoTree == params.ownerErgoTree)
          .flatMap { output =>
            val base = params.baseAsset
            val rewardAmountM =
              if (base == constants.NativeAssetId) Some(output.value)
              else output.assets.find(_.tokenId == base).map(_.amount)
            rewardAmountM.map { rewardAmount =>
              val price  = params.price
              val amount = rewardAmount / price
              Trade(
                Side.Sell,
                tx.id,
                tx.inclusionHeight,
                params.quoteAsset,
                params.baseAsset,
                amount,
                price,
                params.feePerToken,
                tx.timestamp
              )
            }
          }
        acc ++ trades
      }

    private def collectBuyTrades(tx: Transaction, bids: List[OrderParams]): List[Trade] =
      bids.foldLeft(List.empty[Trade]) { case (acc, params) =>
        val trades = tx.outputs
          .find(out => out.ergoTree == params.ownerErgoTree)
          .flatMap { output =>
            val quote = params.quoteAsset
            output.assets
              .find(_.tokenId == quote)
              .map(_.amount)
              .map { purchaseAmount =>
                Trade(
                  Side.Buy,
                  tx.id,
                  tx.inclusionHeight,
                  params.quoteAsset,
                  params.baseAsset,
                  purchaseAmount,
                  params.price,
                  params.feePerToken,
                  tx.timestamp
                )
              }
          }
        acc ++ trades
      }
  }
}
