package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import tofu.logging.derivation.loggable

/** Either executed or cancelled CFMM versioned order.
  */
@derive(encoder, decoder, loggable)
final case class EvaluatedCFMMOrder[A <: CFMMVersionedOrder.Any, E <: OrderEvaluation](
  order: A,
  eval: Option[E],
  pool: Option[CFMMPool],
  orderExecutorFee: Option[OrderExecutorFee]
)

object EvaluatedCFMMOrder {
  type Any = EvaluatedCFMMOrder[CFMMVersionedOrder.Any, OrderEvaluation]
}
