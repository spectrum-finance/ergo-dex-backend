package org.ergoplatform.dex.markets.api.v1.models.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype
import sttp.tapir.Schema
import scala.math.BigDecimal.RoundingMode

object types {

  @derive(encoder, decoder)
  @newtype case class RealPrice(value: BigDecimal) {
    def setScale(scale: Int): RealPrice = RealPrice(value.setScale(scale, RoundingMode.HALF_UP))
  }

  object RealPrice {

    def calculate(
      baseAssetAmount: Long,
      baseAssetDecimals: Option[Int],
      quoteAssetAmount: Long,
      quoteAssetDecimals: Option[Int]
    ): RealPrice =
      RealPrice(
        BigDecimal(quoteAssetAmount) / baseAssetAmount * BigDecimal(10)
          .pow(
            baseAssetDecimals.getOrElse(0) - quoteAssetDecimals.getOrElse(0)
          )
      )

    implicit val realPriceSchema: Schema[RealPrice] = deriving
  }
}
