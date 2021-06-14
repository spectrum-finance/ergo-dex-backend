package org.ergoplatform.ergo.models

import derevo.circe.encoder
import derevo.derive
import io.circe.Decoder
import org.ergoplatform.common.HexString
import tofu.logging.derivation.loggable

@derive(encoder, loggable)
sealed trait SConstant

object SConstant {

  @derive(encoder, loggable)
  final case class IntConstant(value: Int) extends SConstant

  @derive(encoder, loggable)
  final case class LongConstant(value: Long) extends SConstant

  @derive(encoder, loggable)
  final case class ByteaConstant(value: HexString) extends SConstant

  implicit val decoder: Decoder[SConstant] = { c =>
    c.downField("renderedValue").as[String].flatMap { value =>
      c.downField("sigmaType").as[String].map {
        case "SInt"               => IntConstant(value.toInt)
        case "SLong"              => LongConstant(value.toLong)
        case "SCollection[SByte]" => ByteaConstant(HexString.unsafeFromString(value))
      }
    }
  }
}
