package org.ergoplatform.ergo.domain

import derevo.cats.show
import derevo.circe.encoder
import derevo.derive
import io.circe.{Decoder, Encoder, Json}
import org.ergoplatform.common.HexString
import org.ergoplatform.ergo.PubKey
import org.ergoplatform.ergo.domain.SigmaType.SimpleKindSigmaType._
import org.ergoplatform.ergo.domain.SigmaType._
import tofu.logging.derivation.loggable
import io.circe.syntax._

@derive(show, loggable)
sealed trait SConstant

object SConstant {

  @derive(loggable, show)
  final case class IntConstant(value: Int) extends SConstant

  @derive(loggable, show)
  final case class LongConstant(value: Long) extends SConstant

  @derive(loggable, show)
  final case class ByteaConstant(value: HexString) extends SConstant

  @derive(loggable, show)
  final case class SigmaPropConstant(value: PubKey) extends SConstant

  @derive(loggable, show)
  final case class UnresolvedConstant(raw: String) extends SConstant

  @derive(loggable, show)
  final case class IntsConstant(value: List[Int]) extends SConstant

  implicit val encoderSConstant: Encoder[SConstant] = { c =>
    val (renderedValue, sigmaType: SigmaType) = c match {
      case IntConstant(value)       => value.toString                    -> SInt
      case LongConstant(value)      => value.toString                    -> SLong
      case ByteaConstant(value)     => value.value.value                 -> SCollection(SByte)
      case IntsConstant(value)      => "[" ++ value.mkString(",") ++ "]" -> SCollection(SInt)
      case SigmaPropConstant(value) => value.value.value.value           -> SSigmaProp
      case UnresolvedConstant(raw)  => raw                               -> SAny
    }
    Json.obj("renderedValue" -> Json.fromString(renderedValue), "sigmaType" -> sigmaType.asJson)
  }

  implicit val decoderSConstant: Decoder[SConstant] = { c =>
    c.downField("renderedValue").as[String].flatMap { value =>
      c.downField("sigmaType").as[SigmaType].map {
        case SInt               => IntConstant(value.toInt)
        case SLong              => LongConstant(value.toLong)
        case SSigmaProp         => SigmaPropConstant(PubKey.unsafeFromString(value))
        case SCollection(SByte) => ByteaConstant(HexString.unsafeFromString(value))
        case SCollection(SInt)  => parseSInt(value)
        case _                  => UnresolvedConstant(value)
      }
    }
  }

  def fromRenderValue(sType: SigmaType, value: String): SConstant =
    sType match {
      case SInt               => IntConstant(value.toInt)
      case SLong              => LongConstant(value.toLong)
      case SSigmaProp         => SigmaPropConstant(PubKey.unsafeFromString(value))
      case SCollection(SByte) => ByteaConstant(HexString.unsafeFromString(value))
      case SCollection(SInt)  => parseSInt(value)
      case _                  => UnresolvedConstant(value)
    }

  def parseSInt(value: String): IntsConstant = {
    val split = value.split(",")
    if (split.length == 1) {
      val splitHeadTail = split.headOption.map(_.drop(1).dropRight(1)).getOrElse("")
      if (splitHeadTail.isEmpty) IntsConstant(List.empty)
      else IntsConstant(List(splitHeadTail).map(_.toInt))
    } else {
      val splitHead  = split.headOption.map(_.drop(1)).getOrElse("")
      val splitTail  = split.lastOption.map(_.dropRight(1)).getOrElse("")
      val splitList  = split.drop(1).dropRight(1).toList
      val splitTotal = (splitHead :: splitList) :+ splitTail
      IntsConstant(splitTotal.map(_.toInt))
    }

  }
}
