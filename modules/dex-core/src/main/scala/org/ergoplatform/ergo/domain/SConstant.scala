package org.ergoplatform.ergo.domain

import derevo.cats.show
import derevo.circe.encoder
import derevo.derive
import io.circe.Decoder
import org.ergoplatform.common.HexString
import org.ergoplatform.ergo.PubKey
import org.ergoplatform.ergo.domain.SigmaType.SimpleKindSigmaType._
import org.ergoplatform.ergo.domain.SigmaType._
import tofu.logging.derivation.loggable

@derive(show, encoder, loggable)
sealed trait SConstant

object SConstant {

  @derive(show, encoder, loggable)
  final case class IntConstant(value: Int) extends SConstant

  @derive(show, encoder, loggable)
  final case class LongConstant(value: Long) extends SConstant

  @derive(show, encoder, loggable)
  final case class ByteaConstant(value: HexString) extends SConstant

  @derive(show, encoder, loggable)
  final case class SigmaPropConstant(value: PubKey) extends SConstant

  @derive(show, encoder, loggable)
  final case class UnresolvedConstant(raw: String) extends SConstant

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
}
