package org.ergoplatform

import cats.instances.either._
import cats.syntax.either._
import cats.syntax.functor._
import cats.{Applicative, Show}
import derevo.derive
import doobie.{Get, Put}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import eu.timepit.refined.string.{HexStringSpec, Url}
import io.circe.refined._
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import org.ergoplatform.common.errors.RefinementFailed
import org.ergoplatform.ergo.constraints.{HexStringType, UrlStringType}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import scorex.util.encode.Base16
import sttp.tapir.{Codec, CodecFormat, DecodeResult, Schema, Validator}
import tofu.logging.Loggable
import tofu.logging.derivation.loggable
import tofu.syntax.raise._
import tofu.{Raise, WithContext, WithLocal}

package object common {

  @newtype case class HexString(value: HexStringType) {
    final def unwrapped: String    = value.value
    final def toBytes: Array[Byte] = Base16.decode(unwrapped).get
  }

  object HexString {
    // circe instances
    implicit val encoder: Encoder[HexString] = deriving
    implicit val decoder: Decoder[HexString] = deriving

    implicit val show: Show[HexString]         = _.unwrapped
    implicit val loggable: Loggable[HexString] = Loggable.show

    implicit def plainCodec: Codec.PlainCodec[HexString] =
      deriveCodec[String, CodecFormat.TextPlain, HexString](
        fromString[Either[Throwable, *]](_),
        _.unwrapped
      )

    implicit def schema: Schema[HexString] =
      Schema.schemaForString.description("Hex-encoded string").asInstanceOf[Schema[HexString]]

    implicit def validator: Validator[HexString] =
      Schema.schemaForString.validator.contramap[HexString](_.unwrapped)

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

  @derive(loggable)
  @newtype case class TraceId(value: String)

  object TraceId {
    type Local[F[_]] = WithLocal[F, TraceId]
    type Has[F[_]]   = WithContext[F, TraceId]

    def fromString(s: String): TraceId       = apply(s)
  }

  private def deriveCodec[A, CF <: CodecFormat, T](
    at: A => Either[Throwable, T],
    ta: T => A
  )(implicit c: Codec[String, A, CF]): Codec[String, T, CF] =
    c.mapDecode { x =>
      at(x).fold(DecodeResult.Error(x.toString, _), DecodeResult.Value(_))
    }(ta)
}
