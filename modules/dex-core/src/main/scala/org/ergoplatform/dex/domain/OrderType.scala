package org.ergoplatform.dex.domain

import enumeratum._

sealed abstract class OrderType(override val entryName: String) extends EnumEntry

object OrderType extends Enum[OrderType] with CirceEnum[OrderType] {
  case object Buy extends OrderType("buy")
  case object Sell extends OrderType("sell")

  val values = findValues
}
