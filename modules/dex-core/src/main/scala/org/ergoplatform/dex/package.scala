package org.ergoplatform

import cats.Applicative
import cats.instances.either._
import cats.syntax.either._
import cats.syntax.functor._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{HexStringSpec, MatchesRegex, Url}
import eu.timepit.refined.{refineV, W}
import io.circe.refined._
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import org.ergoplatform.dex.Err.RefinementFailed
import org.ergoplatform.dex.constraints.{AddressType, Base58Spec, HexStringType, UrlStringType}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import tofu.Raise
import tofu.logging.Loggable
import tofu.syntax.raise._

package object dex {

  object constraints {

    type Base58Spec = MatchesRegex[W.`"[1-9A-HJ-NP-Za-km-z]+"`.T]

    type AddressType = String Refined Base58Spec

    type HexStringType = String Refined HexStringSpec

    type UrlStringType = String Refined Url
  }

  @newtype case class OrderId(value: String)

  @newtype case class TxId(value: String)

  object TxId {
    // circe instances
    implicit val encoder: Encoder[TxId] = deriving
    implicit val decoder: Decoder[TxId] = deriving
  }

  @newtype case class BoxId(value: String)

  object BoxId {
    // circe instances
    implicit val encoder: Encoder[BoxId] = deriving
    implicit val decoder: Decoder[BoxId] = deriving
  }

  @newtype case class AssetId(value: HexString) {
    def unwrapped: String = value.unwrapped
  }

  object AssetId {
    // circe instances
    implicit val encoder: Encoder[AssetId] = deriving
    implicit val decoder: Decoder[AssetId] = deriving

    def fromString[F[_]: Raise[*[_], RefinementFailed]: Applicative](
      s: String
    ): F[AssetId] =
      HexString.fromString(s).map(AssetId.apply)

    def fromBytes(bytes: Array[Byte]): AssetId =
      AssetId(HexString.fromBytes(bytes))
  }

  // Ergo Address
  @newtype case class Address(value: AddressType) {
    final def unwrapped: String = value.value
  }

  object Address {
    // circe instances
    implicit val encoder: Encoder[Address] = deriving
    implicit val decoder: Decoder[Address] = deriving

    def fromString[F[_]: Raise[*[_], RefinementFailed]: Applicative](
      s: String
    ): F[Address] =
      refineV[Base58Spec](s)
        .leftMap(RefinementFailed)
        .toRaise[F]
        .map(Address.apply)
  }

  @newtype case class HexString(value: HexStringType) {
    final def unwrapped: String = value.value
  }

  object HexString {
    // circe instances
    implicit val encoder: Encoder[HexString] = deriving
    implicit val decoder: Decoder[HexString] = deriving

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
          .leftMap(e => CannotConvert(s, s"Refined", e.msg))
      }

    def fromString[F[_]: Raise[*[_], RefinementFailed]: Applicative](
      s: String
    ): F[UrlString] =
      refineV[Url](s)
        .leftMap(RefinementFailed)
        .toRaise[F]
        .map(UrlString.apply)
  }

  @newtype case class PairId(value: String)

  object PairId {
    // circe instances
    implicit val encoder: Encoder[PairId] = deriving
    implicit val decoder: Decoder[PairId] = deriving

    implicit val loggable: Loggable[PairId] = deriving
  }
}
