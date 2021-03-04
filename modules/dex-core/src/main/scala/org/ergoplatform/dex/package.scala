package org.ergoplatform

import cats.effect.Sync
import cats.instances.either._
import cats.instances.string._
import cats.syntax.either._
import cats.syntax.functor._
import cats.{Applicative, Show}
import derevo.derive
import doobie._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{HexStringSpec, MatchesRegex, Url}
import eu.timepit.refined.{refineV, W}
import fs2.kafka.instances._
import fs2.kafka.{RecordDeserializer, RecordSerializer}
import io.circe.refined._
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import org.ergoplatform.dex.{AssetId, BoxId, HexString}
import org.ergoplatform.dex.constraints.{AddressType, Base58Spec, HexStringType, UrlStringType}
import org.ergoplatform.dex.errors.RefinementFailed
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import scorex.util.encode.Base16
import tofu.Raise
import tofu.logging.Loggable
import tofu.logging.derivation.loggable
import tofu.syntax.raise._

package object dex {

  object constraints {

    type Base58Spec = MatchesRegex[W.`"[1-9A-HJ-NP-Za-km-z]+"`.T]

    type AddressType = String Refined Base58Spec

    type HexStringType = String Refined HexStringSpec

    type UrlStringType = String Refined Url
  }

  @newtype case class OrderId(value: String)

  object OrderId {

    implicit val loggable: Loggable[OrderId] = deriving

    implicit val get: Get[OrderId] = deriving
    implicit val put: Put[OrderId] = deriving

    // circe instances
    implicit val encoder: Encoder[OrderId] = deriving
    implicit val decoder: Decoder[OrderId] = deriving

    implicit def recordSerializer[F[_]: Sync]: RecordSerializer[F, OrderId]     = serializerByEncoder
    implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, OrderId] = deserializerByDecoder
  }

  @newtype case class TradeId(value: String)

  object TradeId {

    implicit val loggable: Loggable[TradeId] = deriving

    implicit val get: Get[TradeId] = deriving
    implicit val put: Put[TradeId] = deriving

    // circe instances
    implicit val encoder: Encoder[TradeId] = deriving
    implicit val decoder: Decoder[TradeId] = deriving

    implicit def recordSerializer[F[_]: Sync]: RecordSerializer[F, TradeId]     = serializerByEncoder
    implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, TradeId] = deserializerByDecoder
  }

  @newtype case class TxId(value: String)

  object TxId {

    implicit val loggable: Loggable[TxId] = deriving

    // circe instances
    implicit val encoder: Encoder[TxId] = deriving
    implicit val decoder: Decoder[TxId] = deriving

    implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, TxId] = deserializerByDecoder
  }

  @newtype case class BlockId(value: String)

  object BlockId {

    implicit val loggable: Loggable[BlockId] = deriving

    // circe instances
    implicit val encoder: Encoder[BlockId] = deriving
    implicit val decoder: Decoder[BlockId] = deriving

    implicit val get: Get[BlockId] = deriving
    implicit val put: Put[BlockId] = deriving
  }

  @newtype case class BoxId(value: String)

  object BoxId {

    implicit val show: Show[BoxId]         = deriving
    implicit val loggable: Loggable[BoxId] = deriving

    // circe instances
    implicit val encoder: Encoder[BoxId] = deriving
    implicit val decoder: Decoder[BoxId] = deriving

    implicit val get: Get[BoxId] = deriving
    implicit val put: Put[BoxId] = deriving

    def fromErgo(ergoBoxId: ErgoBox.BoxId): BoxId =
      Base16.encode(ergoBoxId).coerce[BoxId]
  }

  @newtype case class AssetId(value: HexString) {
    def unwrapped: String = value.unwrapped
  }

  object AssetId {
    // circe instances
    implicit val encoder: Encoder[AssetId] = deriving
    implicit val decoder: Decoder[AssetId] = deriving

    implicit val get: Get[AssetId] =
      Get[HexString].map(AssetId(_))

    implicit val put: Put[AssetId] =
      Put[String].contramap[AssetId](_.unwrapped)

    implicit val show: Show[AssetId]         = _.unwrapped
    implicit val loggable: Loggable[AssetId] = Loggable.show

    def fromString[F[_]: Raise[*[_], RefinementFailed]: Applicative](
      s: String
    ): F[AssetId] =
      HexString.fromString(s).map(AssetId.apply)

    def fromBytes(bytes: Array[Byte]): AssetId =
      AssetId(HexString.fromBytes(bytes))
  }

  @newtype case class TokenType(value: String)

  object TokenType {
    // circe instances
    implicit val encoder: Encoder[TokenType] = deriving
    implicit val decoder: Decoder[TokenType] = deriving

    implicit val get: Get[TokenType] = deriving
    implicit val put: Put[TokenType] = deriving

    implicit val show: Show[TokenType]         = _.value
    implicit val loggable: Loggable[TokenType] = Loggable.show
  }

  // Ergo Address
  @newtype case class Address(value: AddressType) {
    final def unwrapped: String = value.value
  }

  object Address {

    implicit val show: Show[Address]         = _.unwrapped
    implicit val loggable: Loggable[Address] = Loggable.show

    // circe instances
    implicit val encoder: Encoder[Address] = deriving
    implicit val decoder: Decoder[Address] = deriving

    implicit val configReader: ConfigReader[Address] = implicitly[ConfigReader[String]].emap { raw =>
      fromString[Either[Throwable, *]](raw).leftMap(e => CannotConvert(raw, "Address", e.getMessage))
    }

    def fromString[F[_]: Raise[*[_], RefinementFailed]: Applicative](
      s: String
    ): F[Address] =
      refineV[Base58Spec](s)
        .leftMap(RefinementFailed)
        .toRaise[F]
        .map(Address.apply)

    def fromStringUnsafe(s: String): Address =
      Address(refineV[Base58Spec].unsafeFrom(s))
  }

  @derive(loggable)
  final case class PairId(quoteId: AssetId, baseId: AssetId)

  @newtype case class HexString(value: HexStringType) {
    final def unwrapped: String    = value.value
    final def toBytea: Array[Byte] = Base16.decode(unwrapped).get
  }

  object HexString {
    // circe instances
    implicit val encoder: Encoder[HexString] = deriving
    implicit val decoder: Decoder[HexString] = deriving

    implicit val show: Show[HexString]         = _.unwrapped
    implicit val loggable: Loggable[HexString] = Loggable.show

    implicit val get: Get[HexString] =
      Get[String]
        .temap(s => refineV[HexStringSpec](s))
        .map(rs => HexString(rs))

    implicit val put: Put[HexString] =
      Put[String].contramap[HexString](_.unwrapped)

    def fromString[F[_]: Raise[*[_], RefinementFailed]: Applicative](
      s: String
    ): F[HexString] =
      refineV[HexStringSpec](s)
        .leftMap(RefinementFailed)
        .toRaise[F]
        .map(HexString.apply)

    def fromBytes(bytes: Array[Byte]): HexString =
      unsafeFromString(scorex.util.encode.Base16.encode(bytes))

    def unsafeFromString(s: String): HexString = HexString(Refined.unsafeApply(s))
  }

  @newtype case class ErgoTree(value: HexString) {
    final def unwrapped: String    = value.unwrapped
    final def toBytea: Array[Byte] = value.toBytea
  }

  object ErgoTree {
    // circe instances
    implicit val encoder: Encoder[ErgoTree] = deriving
    implicit val decoder: Decoder[ErgoTree] = deriving

    implicit val show: Show[ErgoTree]         = deriving
    implicit val loggable: Loggable[ErgoTree] = deriving

    implicit val get: Get[ErgoTree] = deriving
    implicit val put: Put[ErgoTree] = deriving

    def fromBytes(bytes: Array[Byte]): ErgoTree = ErgoTree(HexString.fromBytes(bytes))

    def fromString[F[_]: Raise[*[_], RefinementFailed]: Applicative](
      s: String
    ): F[ErgoTree] = HexString.fromString(s).map(ErgoTree.apply)

    def unsafeFromString(s: String): ErgoTree = ErgoTree(HexString.unsafeFromString(s))
  }

  @newtype case class UrlString(value: UrlStringType) {
    final def unwrapped: String = value.value
  }

  object UrlString {
    // circe instances
    implicit val encoder: Encoder[UrlString] = deriving
    implicit val decoder: Decoder[UrlString] = deriving

    implicit val configReader: ConfigReader[UrlString] =
      implicitly[ConfigReader[String]].emap { s =>
        fromString[Either[RefinementFailed, *]](s)
          .leftMap(e => CannotConvert(s, s"Refined", e.getMessage))
      }

    def fromString[F[_]: Raise[*[_], RefinementFailed]: Applicative](
      s: String
    ): F[UrlString] =
      refineV[Url](s)
        .leftMap(RefinementFailed)
        .toRaise[F]
        .map(UrlString.apply)
  }

  @newtype case class TraceId(value: String)

  object TraceId {
    implicit val loggable: Loggable[TraceId] = deriving
  }
}
