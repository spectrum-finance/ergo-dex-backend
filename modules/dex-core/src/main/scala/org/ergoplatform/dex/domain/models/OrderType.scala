package org.ergoplatform.dex.domain.models

import enumeratum._

sealed abstract class OrderType(override val entryName: String) extends EnumEntry {
  def isAsk = false
}

object OrderType extends Enum[OrderType] with CirceEnum[OrderType] {
  case object Ask extends OrderType("ASK") { override def isAsk: Boolean = true }
  case object Bid extends OrderType("BID")

  type Ask = Ask.type
  type Bid  = Bid.type

  val values = findValues
}
