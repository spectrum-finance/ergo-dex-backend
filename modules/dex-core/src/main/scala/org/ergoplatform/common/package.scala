package org.ergoplatform

import cats.instances.either._
import cats.syntax.either._
import cats.syntax.functor._
import cats.{Applicative, Show}
import doobie.{Get, Put}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineV
import eu.timepit.refined.string.{HexStringSpec, Url}
import io.circe.refined._
import io.circe.{Decoder, Encoder}
import io.estatico.newtype.macros.newtype
import org.ergoplatform.dex.errors.RefinementFailed
import org.ergoplatform.ergo.constraints.{HexStringType, UrlStringType}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import scorex.util.encode.Base16
import tofu.Raise
import tofu.logging.Loggable
import tofu.syntax.raise._

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

  @newtype case class TraceId(value: String)

  object TraceId {
    implicit val loggable: Loggable[TraceId] = deriving
  }
}
