package org.ergoplatform.dex.markets.models

import cats.syntax.either._
import derevo.derive
import doobie.util.{Get, Put}
import enumeratum._
import tofu.logging.derivation.loggable

@derive(loggable)
sealed trait Side extends EnumEntry

object Side extends Enum[Side] {

  case object Sell extends Side
  case object Buy extends Side

  val values = findValues

  implicit val put: Put[Side] = Put[String].contramap(_.entryName)

  implicit val get: Get[Side] = Get[String].temap(s =>
    withNameEither(s).leftMap(nsm => s"'${nsm.notFoundName}' doesn't match any of ${nsm.enumValues.mkString(", ")}")
  )
}
