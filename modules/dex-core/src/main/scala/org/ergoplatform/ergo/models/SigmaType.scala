package org.ergoplatform.ergo.models

import cats.Eval
import cats.data.{NonEmptyList, OptionT}
import cats.instances.list._
import cats.syntax.either._
import cats.syntax.traverse._
import doobie.util.{Get, Put}
import enumeratum._
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Encoder}

import scala.annotation.tailrec

trait SigmaType

object SigmaType {

  sealed abstract class SimpleKindSigmaType extends EnumEntry with SigmaType

  object SimpleKindSigmaType extends Enum[SimpleKindSigmaType] {
    case object SBoolean extends SimpleKindSigmaType
    case object SByte extends SimpleKindSigmaType
    case object SShort extends SimpleKindSigmaType
    case object SInt extends SimpleKindSigmaType
    case object SLong extends SimpleKindSigmaType
    case object SBigInt extends SimpleKindSigmaType
    case object SContext extends SimpleKindSigmaType
    case object SGlobal extends SimpleKindSigmaType
    case object SHeader extends SimpleKindSigmaType
    case object SPreHeader extends SimpleKindSigmaType
    case object SAvlTree extends SimpleKindSigmaType
    case object SGroupElement extends SimpleKindSigmaType
    case object SSigmaProp extends SimpleKindSigmaType
    case object SString extends SimpleKindSigmaType
    case object SBox extends SimpleKindSigmaType
    case object SUnit extends SimpleKindSigmaType
    case object SAny extends SimpleKindSigmaType

    val values = findValues
  }

  sealed abstract class HigherKinded1SigmaType extends SigmaType { val typeParam: SigmaType }

  final case class SCollection(typeParam: SigmaType) extends HigherKinded1SigmaType
  final case class SOption(typeParam: SigmaType) extends HigherKinded1SigmaType

  final case class STupleN(typeParams: NonEmptyList[SigmaType]) extends SigmaType

  implicit def encoder: Encoder[SigmaType] = render(_).asJson

  implicit def decoder: Decoder[SigmaType] =
    c =>
      c.as[String]
        .flatMap { s =>
          parse(s).fold(DecodingFailure(s"Unknown SigmaType signature: [$s]", c.history).asLeft[SigmaType])(_.asRight)
        }

  implicit def get: Get[SigmaType] =
    Get[String].temap(s => parse(s).fold(s"Unknown SigmaType signature: [$s]".asLeft[SigmaType])(_.asRight))
  implicit def put: Put[SigmaType] = Put[String].contramap(render)

  private val HKType1Pattern    = "^([a-zA-Z]+)\\[(.+)\\]$".r
  private val TupleNTypePattern = "^\\(((?:\\S+,?.)+)\\)$".r

  def parse(s: String): Option[SigmaType] = {
    def in(si: String): OptionT[Eval, SigmaType] =
      OptionT.fromOption[Eval](SimpleKindSigmaType.withNameOption(si): Option[SigmaType]).orElse {
        si match {
          case HKType1Pattern(tpe, tParamRaw) =>
            in(tParamRaw).flatMap { tParam =>
              tpe match {
                case "Coll"   => OptionT.some(SCollection(tParam))
                case "Option" => OptionT.some(SOption(tParam))
                case _        => OptionT.none
              }
            }
          case TupleNTypePattern(tps) =>
            parseHighLevelTypeSigns(tps).traverse(in).map(xs => STupleN(NonEmptyList.fromListUnsafe(xs)))
          case _ => OptionT.none
        }
      }
    in(s).value.value
  }

  private def parseHighLevelTypeSigns(s: String): List[String] = {
    @tailrec def go(
      rem: List[Char],
      bf: List[Char],
      acc: List[String],
      openBrackets: Int,
      closedBrackets: Int
    ): List[String] =
      rem match {
        case h :: tl =>
          h match {
            case '[' => go(tl, bf :+ h, acc, openBrackets + 1, closedBrackets)
            case ']' => go(tl, bf :+ h, acc, openBrackets, closedBrackets + 1)
            case ',' if openBrackets == closedBrackets =>
              go(tl, List.empty, acc :+ bf.mkString.trim, openBrackets, closedBrackets)
            case c => go(tl, bf :+ c, acc, openBrackets, closedBrackets)
          }
        case _ => acc :+ bf.mkString.trim
      }
    go(s.toList, List.empty, List.empty, openBrackets = 0, closedBrackets = 0)
  }

  private def render(t: SigmaType): String = {
    def go(t0: SigmaType): Eval[String] =
      t0 match {
        case st: SimpleKindSigmaType => Eval.now(st.entryName)
        case coll: SCollection       => Eval.defer(go(coll.typeParam)).map(r => s"Coll[$r]")
        case opt: SOption            => Eval.defer(go(opt.typeParam)).map(r => s"Option[$r]")
        case STupleN(tParams) =>
          tParams.traverse(tp => Eval.defer(go(tp))).map(tps => "(" + tps.toList.mkString(", ") + ")")
      }
    go(t).value
  }
}