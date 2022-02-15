package org.ergoplatform.dex.markets

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype
import org.ergoplatform.common.models.TimeWindow
import org.ergoplatform.dex.domain.{CryptoUnits, FiatUnits}
import sttp.tapir.Schema

object domain {

  @derive(encoder, decoder)
  final case class TotalValueLocked(value: BigDecimal, units: FiatUnits)

  implicit val schemaTvl: Schema[TotalValueLocked] = Schema.derived

  @derive(encoder, decoder)
  final case class Volume(value: BigDecimal, units: FiatUnits, window: TimeWindow)

  object Volume {
    implicit val schemaVolume: Schema[Volume]               = Schema.derived
    def empty(units: FiatUnits, window: TimeWindow): Volume = Volume(BigDecimal(0), units, window)
  }

  @derive(encoder, decoder)
  final case class CryptoVolume(value: BigDecimal, units: CryptoUnits, window: TimeWindow)

  object CryptoVolume {
    implicit val schemaVolume: Schema[CryptoVolume]                 = Schema.derived
    def empty(units: CryptoUnits, window: TimeWindow): CryptoVolume = CryptoVolume(BigDecimal(0), units, window)
  }

  @derive(encoder, decoder)
  final case class Fees(value: BigDecimal, units: FiatUnits, window: TimeWindow)

  object Fees {
    implicit val schemaFees: Schema[Fees]                 = Schema.derived
    def empty(units: FiatUnits, window: TimeWindow): Fees = Fees(BigDecimal(0), units, window)
  }

  @derive(encoder, decoder)
  @newtype final case class FeePercentProjection(value: Double)

  object FeePercentProjection {
    implicit val schema: Schema[FeePercentProjection] = deriving
    def empty: FeePercentProjection                   = FeePercentProjection(0.0)
  }
}
