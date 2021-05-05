package org.ergoplatform.dex.executor.amm.domain

import cats.syntax.show._
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.PoolId
import tofu.Errors

object errors {

  abstract class ExecutionFailed(msg: String) extends Exception(msg)

  object ExecutionFailed {
    type Raise[F[_]] = tofu.Raise[F, ExecutionFailed]
  }

  final case class ExhaustedOutputValue(available: Long, required: Long, nanoErgsPerByte: Long)
    extends ExecutionFailed(
      s"Output value exhausted. [Available: $available, Required: $required, NanoErgsPerByte: $nanoErgsPerByte]"
    )

  final case class NoSuchPool(poolId: PoolId) extends ExecutionFailed(s"Pool [$poolId] not found")

  final case class TooMuchSlippage(poolId: PoolId, minOutput: AssetAmount, actualOutput: AssetAmount)
    extends ExecutionFailed(
      s"Too much slippage for pool [$poolId]; minOutput: [${minOutput.show}], actualOutput: [${actualOutput.show}]"
    )
}
