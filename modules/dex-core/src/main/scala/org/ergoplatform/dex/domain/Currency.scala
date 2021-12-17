package org.ergoplatform.dex.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.CurrencyId
import sttp.tapir.Schema

@derive(encoder, decoder)
final case class Currency(id: CurrencyId, decimals: Int)

object Currency {
  implicit val schema: Schema[Currency] = Schema.derived
}
