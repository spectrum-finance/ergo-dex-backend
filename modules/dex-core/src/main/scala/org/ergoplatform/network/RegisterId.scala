package org.ergoplatform.network

import cats.syntax.either._
import doobie.util.{Get, Put}
import enumeratum.{CirceEnum, Enum, EnumEntry}
import io.circe.{KeyDecoder, KeyEncoder}
import tofu.logging.Loggable

sealed abstract class RegisterId extends EnumEntry

object RegisterId extends Enum[RegisterId] with CirceEnum[RegisterId] {

  case object R0 extends RegisterId
  case object R1 extends RegisterId
  case object R2 extends RegisterId
  case object R3 extends RegisterId
  case object R4 extends RegisterId
  case object R5 extends RegisterId
  case object R6 extends RegisterId
  case object R7 extends RegisterId
  case object R8 extends RegisterId
  case object R9 extends RegisterId

  val values = findValues

  implicit val loggable: Loggable[RegisterId] = Loggable.stringValue.contramap(_.entryName)

  implicit val keyDecoder: KeyDecoder[RegisterId] = withNameOption
  implicit val keyEncoder: KeyEncoder[RegisterId] = _.entryName

  implicit val get: Get[RegisterId] =
    Get[String].temap(s => withNameEither(s).leftMap(_ => s"No such RegisterId [$s]"))

  implicit val put: Put[RegisterId] =
    Put[String].contramap[RegisterId](_.entryName)
}
