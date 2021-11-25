package org.ergoplatform.dex.index.repos

import org.ergoplatform.dex.domain.amm.{Deposit, Redeem, Swap}

trait CFMMRepo[F[_]] {
  def insertSwaps(swaps: List[Swap]): F[Int]
  def insertDeposits(deposits: List[Deposit]): F[Int]
  def insertRedeems(redeems: List[Redeem]): F[Int]
}
