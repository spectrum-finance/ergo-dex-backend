package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.FiatUnits
import org.ergoplatform.dex.markets.currencies.UsdUnits
import sttp.tapir.Schema
import scala.math.BigDecimal.RoundingMode

@derive(encoder, decoder)
case class TransactionsInfo(numTxs: Int, avgTxValue: BigDecimal, maxTxValue: BigDecimal, units: FiatUnits) {
  def roundAvgValue: TransactionsInfo = TransactionsInfo(numTxs, avgTxValue.setScale(0, RoundingMode.DOWN), maxTxValue, units)
}

object TransactionsInfo {

  def empty: TransactionsInfo = TransactionsInfo(0, 0, 0, UsdUnits)

  implicit val schemaTrxInfo: Schema[TransactionsInfo] = Schema.derived
}
