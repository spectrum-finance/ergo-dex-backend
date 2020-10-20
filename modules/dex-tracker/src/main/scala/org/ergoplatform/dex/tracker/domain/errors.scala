package org.ergoplatform.dex.tracker.domain

import org.ergoplatform.dex.AssetId
import sigmastate.Values.ErgoTree

object errors {

  abstract class OrderError(val msg: String) extends Throwable

  final case class BadParams(ergoTree: ErgoTree) extends OrderError(s"Bad params: $ergoTree")

  final case class AssetNotProvided(assetId: AssetId) extends OrderError(s"Declared asset not provided: $assetId")

  final case class FeeNotSatisfied(requiredAmount: Long, availableAmount: Long)
    extends OrderError(s"Fee amount not satisfied. Required amount: [$requiredAmount], available: [$availableAmount]")
}
