package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.FiatUnits
import sttp.tapir.Schema

@derive(encoder, decoder)
case class TransactionsInfo(values: List[BigDecimal], units: FiatUnits)

object TransactionsInfo {
  implicit val schemaTrxInfo: Schema[TransactionsInfo] = Schema.derived
}
