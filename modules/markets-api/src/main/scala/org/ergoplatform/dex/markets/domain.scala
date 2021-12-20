package org.ergoplatform.dex.markets

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.FiatUnits
import sttp.tapir.Schema

object domain {

  @derive(encoder, decoder)
  final case class TotalValueLocked(value: BigDecimal, units: FiatUnits)

  implicit val schemaTvl: Schema[TotalValueLocked] = Schema.derived

  @derive(encoder, decoder)
  final case class Volume(value: BigDecimal, units: FiatUnits, window: TimeWindow)

  implicit val schemaVolume: Schema[Volume] = Schema.derived

  @derive(encoder, decoder)
  final case class Fees(value: BigDecimal, units: FiatUnits, window: TimeWindow)

  implicit val schemaFees: Schema[Fees] = Schema.derived

  @derive(encoder, decoder)
  @newtype final case class FeePercentProjection(value: Double)
  object FeePercentProjection {
    implicit val schema: Schema[FeePercentProjection] = deriving
  }
}
