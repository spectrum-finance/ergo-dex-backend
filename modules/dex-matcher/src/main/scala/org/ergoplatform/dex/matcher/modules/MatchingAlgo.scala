package org.ergoplatform.dex.matcher.modules

import cats.Applicative
import cats.syntax.applicative._
import org.ergoplatform.dex.domain.models.Order.{Ask, Bid}
import org.ergoplatform.dex.domain.models.Trade.AnyTrade
import org.ergoplatform.dex.domain.syntax.order._

import scala.annotation.tailrec
import scala.language.existentials

trait MatchingAlgo[F[_]] {

  def apply(asks: List[Ask], bids: List[Bid]): F[List[AnyTrade]]
}

object MatchingAlgo {

  implicit def instance[F[_]: Applicative]: MatchingAlgo[F] = new PrioritizeByFeeMatchingAlgo[F]

  final class PrioritizeByFeeMatchingAlgo[F[_]: Applicative] extends MatchingAlgo[F] {

    implicit private val askOrd: Ordering[Ask] = Ordering.by(o => (o.price, -o.fee))
    implicit private val bidOrd: Ordering[Bid] = Ordering.by(o => (-o.price, -o.fee))

    def apply(asks: List[Ask], bids: List[Bid]): F[List[AnyTrade]] = {
      @tailrec def matchLoop(
        asks: List[Ask],
        bids: List[Bid],
        trades: List[AnyTrade]
      ): List[AnyTrade] =
        (asks.headOption, bids.headOption) match {
          case (Some(ask), Some(bid)) if ask.price <= bid.price =>
            if (ask.fee > bid.fee)
              ask fillWith bids match {
                case Some(anyTrade) =>
                  matchLoop(
                    asks.tail,
                    bids.dropWhile(anyTrade.counterOrders.toList.contains),
                    anyTrade +: trades
                  )
                case None =>
                  trades
              }
            else
              bid fillWith asks match {
                case Some(anyTrade) =>
                  matchLoop(
                    asks.dropWhile(anyTrade.counterOrders.toList.contains),
                    bids.tail,
                    anyTrade +: trades
                  )
                case None =>
                  trades
              }
          case _ => trades
        }
      matchLoop(asks.sorted, bids.sorted, Nil).pure
    }
  }
}
