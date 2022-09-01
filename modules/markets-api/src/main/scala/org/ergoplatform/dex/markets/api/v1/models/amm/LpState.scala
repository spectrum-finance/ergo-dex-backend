package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(loggable, decoder, encoder)
final case class LpState(
  pool: String,
  txId: String,
  lpBalance: BigDecimal,
  timestamp: String,
  op: String,
  amount: BigDecimal
)

object LpState {
  implicit val LpStateSchema: Schema[LpState] = Schema.derived
}