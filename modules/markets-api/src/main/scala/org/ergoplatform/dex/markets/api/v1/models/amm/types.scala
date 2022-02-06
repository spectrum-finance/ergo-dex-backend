package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype
import org.ergoplatform.dex.domain.FullAsset
import sttp.tapir.Schema

object types {

  @derive(encoder, decoder)
  @newtype case class RealPrice(value: BigDecimal)

  object RealPrice {

    def calculate(baseAsset: FullAsset, quoteAsset: FullAsset): RealPrice =
      RealPrice(
        BigDecimal(quoteAsset.amount) / baseAsset.amount * BigDecimal(10).pow(
          baseAsset.evalDecimals - quoteAsset.evalDecimals
        )
      )

    implicit val realPriceSchema: Schema[RealPrice] = deriving
  }
}
