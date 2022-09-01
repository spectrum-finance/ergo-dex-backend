package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(loggable, encoder, decoder)
final case class OffChainSpfReward(address: String, spfReward: BigDecimal, operations: Int, totalFee: BigDecimal)

object OffChainSpfReward {

  def empty(address: String): OffChainSpfReward = OffChainSpfReward(address, 0, 0, 0)

  implicit val schemaOffChainReward: Schema[OffChainSpfReward] = Schema.derived
}
