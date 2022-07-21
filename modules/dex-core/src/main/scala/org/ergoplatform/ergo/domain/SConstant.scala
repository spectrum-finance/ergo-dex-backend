package org.ergoplatform.ergo.domain

import derevo.circe.encoder
import derevo.derive
import cats.syntax.either._
import io.circe.Decoder
import org.ergoplatform.common.HexString
import org.ergoplatform.ergo.PubKey
import org.ergoplatform.ergo.domain.SigmaType.SimpleKindSigmaType._
import org.ergoplatform.ergo.domain.SigmaType._
import scodec.{Attempt, Codec, Err}
import scodec.codecs._
import scodec.codecs.uint8
import tofu.logging.derivation.loggable

@derive(encoder, loggable)
sealed trait SConstant

object SConstant {

  @derive(encoder, loggable)
  final case class IntConstant(value: Int) extends SConstant

  object IntConstant {
    implicit val codec: Codec[IntConstant] = int32.as[IntConstant]
  }

  @derive(encoder, loggable)
  final case class LongConstant(value: Long) extends SConstant

  object LongConstant {
    implicit val codec: Codec[LongConstant] = int64.as[LongConstant]
  }

  @derive(encoder, loggable)
  final case class ByteaConstant(value: HexString) extends SConstant

  object ByteaConstant {

    implicit val codec: Codec[ByteaConstant] =
      scodec.codecs
        .variableSizeBits(uint16, utf8)
        .exmap(
          str =>
            Attempt.fromEither(
              Either
                .catchNonFatal(HexString.unsafeFromString(str))
                .map(ByteaConstant(_))
                .leftMap(err => Err(err.getMessage))
            ),
          const => Attempt.successful(const.value.unwrapped)
        )
  }

  @derive(encoder, loggable)
  final case class SigmaPropConstant(value: PubKey) extends SConstant

  object SigmaPropConstant {
    implicit val codec: Codec[SigmaPropConstant] = implicitly[Codec[PubKey]].as[SigmaPropConstant]
  }

  @derive(encoder, loggable)
  final case class UnresolvedConstant(raw: String) extends SConstant

  object UnresolvedConstant {

    implicit val codec: Codec[UnresolvedConstant] =
      scodec.codecs
        .variableSizeBits(uint16, utf8)
        .xmap(UnresolvedConstant(_), _.raw)
  }

  implicit val decoder: Decoder[SConstant] = { c =>
    c.downField("renderedValue").as[String].flatMap { value =>
      c.downField("sigmaType").as[SigmaType].map {
        case SInt               => IntConstant(value.toInt)
        case SLong              => LongConstant(value.toLong)
        case SSigmaProp         => SigmaPropConstant(PubKey.unsafeFromString(value))
        case SCollection(SByte) => ByteaConstant(HexString.unsafeFromString(value))
        case _                  => UnresolvedConstant(value)
      }
    }
  }

  implicit val codec: Codec[SConstant] =
    Codec.coproduct[SConstant].discriminatedByIndex(uint8)
}
