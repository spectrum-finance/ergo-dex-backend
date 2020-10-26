package org.ergoplatform.dex.domain.models

import cats.instances.string._
import cats.syntax.either._
import doobie.{Get, Put}
import enumeratum._

import scala.collection.immutable

sealed trait OrderType extends EnumEntry {
  def isAsk = false
}

object OrderType extends Enum[OrderType] {

  case object Ask extends OrderType { override def isAsk: Boolean = true }
  case object Bid extends OrderType

  type Ask = Ask.type
  type Bid = Bid.type

  val values: immutable.IndexedSeq[OrderType] = findValues

  implicit def get: Get[OrderType] =
    Get[String].temap(s => withNameInsensitiveEither(s).leftMap(_.getMessage))

  implicit def put: Put[OrderType] =
    Put[String].contramap[OrderType](_.entryName)
}
