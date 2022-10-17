package org.ergoplatform.dex.domain.amm

sealed trait CFMMOrderType

object CFMMOrderType {
  sealed trait Swap extends CFMMOrderType
  sealed trait Redeem extends CFMMOrderType
  sealed trait Deposit extends CFMMOrderType
}