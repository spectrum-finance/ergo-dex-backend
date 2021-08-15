package org.ergoplatform

import cats.effect.Sync
import cats.instances.either._
import cats.instances.string._
import cats.syntax.either._
import cats.syntax.functor._
import cats.{Applicative, Show}
import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import doobie._
import scodec.codecs._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{HexStringSpec, MatchesRegex, Url}
import eu.timepit.refined.{W, refineV}
import fs2.kafka.RecordDeserializer
import fs2.kafka.serde._
import io.circe.refined._
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import org.ergoplatform.common.HexString
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.dex.domain.amm.PoolId.fromBytes
import org.ergoplatform.common.errors.RefinementFailed
import org.ergoplatform.ergo.TokenId.fromBytes
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import scodec.bits.ByteVector
import scorex.crypto.hash.Sha256
import scorex.util.encode.Base16
import sttp.tapir.{Codec, Schema, Validator}
import tofu.Raise
import tofu.logging.Loggable
import tofu.logging.derivation.loggable
import tofu.syntax.raise._

package object ergo {

  object constraints {

    type Base58Spec = MatchesRegex[W.`"[1-9A-HJ-NP-Za-km-z]+"`.T]

    type AddressType = String Refined Base58Spec

    type HexStringType = String Refined HexStringSpec

    type UrlStringType = String Refined Url
  }

  import constraints._

  @newtype case class TxId(value: String)

  object TxId {

    implicit val loggable: Loggable[TxId] = deriving

    // circe instances
    implicit val encoder: Encoder[TxId] = deriving
    implicit val decoder: Decoder[TxId] = deriving

    implicit val get: Get[TxId] = deriving
    implicit val put: Put[TxId] = deriving

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

    implicit def schema: Schema[BoxId] =
      Schema.schemaForString.description("Box ID").asInstanceOf[Schema[BoxId]]

    implicit def validator: Validator[BoxId] = Validator.pass

    implicit val get: Get[BoxId] = deriving
    implicit val put: Put[BoxId] = deriving

    implicit def tapirCodec: sttp.tapir.Codec.PlainCodec[BoxId] = deriving

    implicit def codec: scodec.Codec[BoxId] =
      scodec.codecs.variableSizeBits(uint16, utf8).xmap(BoxId(_), _.value)

    def fromErgo(ergoBoxId: ErgoBox.BoxId): BoxId =
      Base16.encode(ergoBoxId).coerce[BoxId]

    def fromStringUnsafe(s: String): BoxId =
      BoxId(s)
  }

  @derive(show, encoder, decoder, loggable)
  @newtype case class TokenId(value: HexString) {
    def unwrapped: String = value.unwrapped
  }

  object TokenId {

    implicit val get: Get[TokenId] =
      Get[HexString].map(TokenId(_))

    implicit val put: Put[TokenId] =
      Put[String].contramap[TokenId](_.unwrapped)

    implicit def plainCodec: Codec.PlainCodec[TokenId] = deriving

    implicit def schema: Schema[TokenId] =
      Schema.schemaForString.description("Token ID").asInstanceOf[Schema[TokenId]]

    implicit def validator: Validator[TokenId] =
      implicitly[Validator[HexString]].contramap[TokenId](_.value)

    implicit def codec: scodec.Codec[TokenId] =
      scodec.codecs
        .bytes(32)
        .xmap(xs => fromBytes(xs.toArray), pid => ByteVector(pid.value.toBytes))

    def fromString[F[_]: Raise[*[_], RefinementFailed]: Applicative](
      s: String
    ): F[TokenId] =
      HexString.fromString(s).map(TokenId.apply)

    def fromStringUnsafe(s: String): TokenId = TokenId(HexString.unsafeFromString(s))

    def fromBytes(bytes: Array[Byte]): TokenId =
      TokenId(HexString.fromBytes(bytes))
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

  @newtype case class SErgoTree(value: HexString) {
    final def unwrapped: String    = value.unwrapped
    final def toBytea: Array[Byte] = value.toBytes
  }

  object SErgoTree {
    // circe instances
    implicit val encoder: Encoder[SErgoTree] = deriving
    implicit val decoder: Decoder[SErgoTree] = deriving

    implicit val show: Show[SErgoTree]         = deriving
    implicit val loggable: Loggable[SErgoTree] = deriving

    implicit val get: Get[SErgoTree] = deriving
    implicit val put: Put[SErgoTree] = deriving

    def fromBytes(bytes: Array[Byte]): SErgoTree = SErgoTree(HexString.fromBytes(bytes))

    def fromString[F[_]: Raise[*[_], RefinementFailed]: Applicative](
      s: String
    ): F[SErgoTree] = HexString.fromString(s).map(SErgoTree.apply)

    def unsafeFromString(s: String): SErgoTree = SErgoTree(HexString.unsafeFromString(s))
  }

  @newtype case class ErgoTreeTemplate(value: HexString) {
    final def unwrapped: String    = value.unwrapped
    final def toBytes: Array[Byte] = value.toBytes
    final def hash: HexString      = HexString.fromBytes(Sha256.hash(toBytes))
  }

  object ErgoTreeTemplate {
    // circe instances
    implicit val encoder: Encoder[ErgoTreeTemplate] = deriving
    implicit val decoder: Decoder[ErgoTreeTemplate] = deriving

    implicit val show: Show[ErgoTreeTemplate]         = deriving
    implicit val loggable: Loggable[ErgoTreeTemplate] = deriving

    implicit val get: Get[ErgoTreeTemplate] = deriving
    implicit val put: Put[ErgoTreeTemplate] = deriving

    def fromBytes(bytes: Array[Byte]): ErgoTreeTemplate = ErgoTreeTemplate(HexString.fromBytes(bytes))

    def fromString[F[_]: Raise[*[_], RefinementFailed]: Applicative](
      s: String
    ): F[ErgoTreeTemplate] = HexString.fromString(s).map(ErgoTreeTemplate.apply)

    def unsafeFromString(s: String): ErgoTreeTemplate = ErgoTreeTemplate(HexString.unsafeFromString(s))
  }
}
