package org.ergoplatform.dex.markets.modules

import cats.Applicative
import org.ergoplatform.network.Transaction
import org.ergoplatform.dex.markets.models.{Fill, Side}
import org.ergoplatform.dex.protocol.orderbook.{OrderContractFamily, OrderContracts, OrderParams}
import org.ergoplatform.dex.protocol.constants
import tofu.syntax.monadic._

trait Fills[F[_], CT <: OrderContractFamily] {

  def extract(tx: Transaction): F[List[Fill]]
}

object Fills {

  implicit def instance[F[_]: Applicative, CT <: OrderContractFamily](implicit scripts: OrderContracts[CT]): Fills[F, CT] =
    new Live[F, CT]

  final class Live[F[_]: Applicative, CT <: OrderContractFamily](implicit scripts: OrderContracts[CT]) extends Fills[F, CT] {

    def extract(tx: Transaction): F[List[Fill]] = {
      val asksIn     = tx.inputs.filter(in => scripts.isAsk(in.ergoTree))
      val asksErased = asksIn.flatMap(scripts.parseAsk)
      val bidsIn     = tx.inputs.filter(in => scripts.isBid(in.ergoTree))
      val bidsErased = bidsIn.flatMap(scripts.parseBid)
      (collectSellFills(tx, asksErased) ++ collectBuyFills(tx, bidsErased)).pure
    }

    private def collectSellFills(tx: Transaction, asks: List[OrderParams]): List[Fill] =
      asks.foldLeft(List.empty[Fill]) { case (acc, params) =>
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
              val fee    = amount * params.feePerToken
              Fill(
                Side.Sell,
                tx.id,
                tx.inclusionHeight,
                params.quoteAsset,
                params.baseAsset,
                amount,
                price,
                fee,
                tx.timestamp
              )
            }
          }
        acc ++ trades
      }

    private def collectBuyFills(tx: Transaction, bids: List[OrderParams]): List[Fill] =
      bids.foldLeft(List.empty[Fill]) { case (acc, params) =>
        val trades = tx.outputs
          .find(out => out.ergoTree == params.ownerErgoTree)
          .flatMap { output =>
            val quote = params.quoteAsset
            output.assets
              .find(_.tokenId == quote)
              .map(_.amount)
              .map { purchaseAmount =>
                val fee = purchaseAmount * params.feePerToken
                Fill(
                  Side.Buy,
                  tx.id,
                  tx.inclusionHeight,
                  params.quoteAsset,
                  params.baseAsset,
                  purchaseAmount,
                  params.price,
                  fee,
                  tx.timestamp
                )
              }
          }
        acc ++ trades
      }
  }
}
