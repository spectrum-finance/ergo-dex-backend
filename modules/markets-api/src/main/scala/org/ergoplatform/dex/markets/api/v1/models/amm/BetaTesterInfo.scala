package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(loggable, encoder, decoder)
final case class BetaTesterInfo(betaTester: Boolean, reward: Int)

object BetaTesterInfo {
  implicit val BetaTesterReq: Schema[BetaTesterInfo] = Schema.derived
}
