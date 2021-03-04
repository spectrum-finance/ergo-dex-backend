package org.ergoplatform.dex.markets.repositories

import cats.data.NonEmptyList
import org.ergoplatform.dex.markets.models.Trade

trait TradesRepo[F[_]] {

  def insert(trade: Trade): F[Unit]

  def insert(trades: NonEmptyList[Trade]): F[Unit]

  def count: F[Int]
}
