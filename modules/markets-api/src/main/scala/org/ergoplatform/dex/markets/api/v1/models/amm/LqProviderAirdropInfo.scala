package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(loggable, decoder, encoder)
final case class LqProviderAirdropInfo(
  totalSpfReward: BigDecimal,
  totalWeight: BigDecimal,
  operations: List[LpState]
)

object LqProviderAirdropInfo {
  implicit val LqProviderAirdropInfoSchema: Schema[LqProviderAirdropInfo] = Schema.derived
}
