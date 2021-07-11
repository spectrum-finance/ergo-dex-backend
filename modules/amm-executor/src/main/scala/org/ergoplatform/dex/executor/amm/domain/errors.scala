package org.ergoplatform.dex.executor.amm.domain

import cats.syntax.show._
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.PoolId
import tofu.Errors

object errors {

  abstract class ExecutionFailed(msg: String) extends Exception(msg)

  object ExecutionFailed extends Errors.Companion[ExecutionFailed]

  final case class ExhaustedOutputValue(available: Long, required: Long, nanoErgsPerByte: Long)
    extends ExecutionFailed(
      s"Output value exhausted. {available=$available, required=$required, nanoErgsPerByte=$nanoErgsPerByte}"
    )

  final case class NoSuchPool(poolId: PoolId) extends ExecutionFailed(s"Pool{id=$poolId} not found")

  final case class PriceTooHigh(poolId: PoolId, minOutput: AssetAmount, actualOutput: AssetAmount)
    extends ExecutionFailed(
      s"Price slipped up too much for Pool{id=$poolId}. {minOutput=${minOutput.show}, actualOutput=${actualOutput.show}}"
    )

  final case class PriceTooLow(poolId: PoolId, maxDexFee: Long, actualDexFee: Long)
    extends ExecutionFailed(
      s"Price slipped down too much for Pool{id=$poolId}. {maxDexFee=${maxDexFee.show}, actualDexFee=${actualDexFee.show}}"
    )
}
