package org.ergoplatform.dex

import cats.Show
import derevo.derive
import doobie.{Get, Put}
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import org.ergoplatform.dex.domain.Ticker
import org.ergoplatform.ergo.TokenId
import scodec.Codec
import scodec.codecs.{uint16, utf8, variableSizeBits}
import sttp.tapir.{Schema, Validator}
import tofu.logging.Loggable
import tofu.logging.derivation.loggable

package object domain {

  @derive(loggable)
  final case class Price(byX: BigDecimal, byY: BigDecimal)

  @derive(loggable)
  final case class PairId(quoteId: TokenId, baseId: TokenId)

  @newtype
  final case class Ticker(value: String)

  object Ticker {

    implicit val get: Get[Ticker] = deriving
    implicit val put: Put[Ticker] = deriving

    implicit val encoder: Encoder[Ticker] = deriving
    implicit val decoder: Decoder[Ticker] = deriving

    implicit val show: Show[Ticker]         = _.value
    implicit val loggable: Loggable[Ticker] = Loggable.show

    implicit val codec: Codec[Ticker] = variableSizeBits(uint16, utf8).xmap[Ticker](Ticker(_), _.value)

    implicit val schema: Schema[Ticker]       = deriving
    implicit val validator: Validator[Ticker] = schema.validator
  }

  @newtype
  final case class MarketId(value: String)

  object MarketId {

    def apply(baseId: TokenId, quoteId: TokenId): MarketId =
      MarketId(s"${baseId}_$quoteId")

    implicit val encoder: Encoder[MarketId] = deriving
    implicit val decoder: Decoder[MarketId] = deriving

    implicit val show: Show[MarketId]         = _.value
    implicit val loggable: Loggable[MarketId] = Loggable.show

    implicit val schema: Schema[MarketId]       = deriving
    implicit val validator: Validator[MarketId] = schema.validator
  }
}
