package org.ergoplatform.dex.tracker.domain

import org.ergoplatform.ergo.TokenId
import sigmastate.Values.ErgoTree

object errors {

  abstract class InvalidOrder(val msg: String) extends Exception(msg)

  final case class BadParams(ergoTree: ErgoTree) extends InvalidOrder(s"Bad params: $ergoTree")

  final case class AssetNotProvided(assetId: TokenId) extends InvalidOrder(s"Declared asset not provided: $assetId")

  final case class FeeNotSatisfied(requiredAmount: Long, availableAmount: Long)
    extends InvalidOrder(s"Fee amount not satisfied. Required amount: [$requiredAmount], available: [$availableAmount]")

  final case class InvalidBidValue(value: Long, price: Long, feePerToken: Long)
    extends InvalidOrder(s"Invalid BID value. Given [$value]. [$value % ($price + $feePerToken) != 0]")
}
