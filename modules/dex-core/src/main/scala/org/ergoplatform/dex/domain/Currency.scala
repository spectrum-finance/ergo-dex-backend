package org.ergoplatform.dex.domain

import cats.Show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.CurrencyId
import sttp.tapir.Schema
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class Currency(id: CurrencyId, decimals: Int)

object Currency {
  implicit val schema: Schema[Currency] = Schema.derived

  implicit val show: Show[Currency] =
    Show.show(c => s"Currency{id=${c.id}, decimals=${c.decimals}]")
}
