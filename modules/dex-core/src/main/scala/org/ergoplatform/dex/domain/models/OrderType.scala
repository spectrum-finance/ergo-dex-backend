package org.ergoplatform.dex.domain.models

import enumeratum._

sealed abstract class OrderType(override val entryName: String) extends EnumEntry {
  def isSell = false
}

object OrderType extends Enum[OrderType] with CirceEnum[OrderType] {
  case object Sell extends OrderType("sell") { override def isSell: Boolean = true }
  case object Buy extends OrderType("buy")

  val values = findValues
}
