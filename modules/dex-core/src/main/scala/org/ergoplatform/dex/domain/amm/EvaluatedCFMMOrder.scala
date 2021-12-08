package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import tofu.logging.derivation.loggable

/** Either executed or cancelled CFMM order.
  */
@derive(encoder, decoder, loggable)
final case class EvaluatedCFMMOrder[A <: CFMMOrder, E <: OrderEvaluation](
  order: A,
  eval: Option[E],
  pool: Option[CFMMPool]
)

object EvaluatedCFMMOrder {
  type Any = EvaluatedCFMMOrder[CFMMOrder, OrderEvaluation]
}
