package org.ergoplatform.dex.domain

import cats.syntax.show._
import cats.Show
import derevo.circe.{decoder, encoder}
import derevo.derive
import sttp.tapir.Schema
import tofu.logging.Loggable
import tofu.logging.derivation.loggable

sealed trait ValueUnits[T]

object ValueUnits {

  implicit def show[T]: Show[ValueUnits[T]] = Show.show {
    case CryptoUnits(asset)  => asset.show
    case FiatUnits(currency) => currency.show
  }

  implicit def loggable[T]: Loggable[ValueUnits[T]] = Loggable.show
}

@derive(encoder, decoder, loggable)
final case class CryptoUnits(asset: AssetClass) extends ValueUnits[AssetClass]

object CryptoUnits {
  implicit val schema: Schema[CryptoUnits] = Schema.derived
}

@derive(encoder, decoder, loggable)
final case class FiatUnits(currency: Currency) extends ValueUnits[Currency]

object FiatUnits {
  implicit val schema: Schema[FiatUnits] = Schema.derived
}
