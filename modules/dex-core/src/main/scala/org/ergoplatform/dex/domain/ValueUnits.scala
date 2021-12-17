package org.ergoplatform.dex.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema

sealed trait ValueUnits[T]

@derive(encoder, decoder)
case class CryptoUnits(asset: AssetClass) extends ValueUnits[AssetClass]

object CryptoUnits {
  implicit val schema: Schema[CryptoUnits] = Schema.derived
}

@derive(encoder, decoder)
case class FiatUnits(currency: Currency) extends ValueUnits[Currency]

object FiatUnits {
  implicit val schema: Schema[FiatUnits] = Schema.derived
}
