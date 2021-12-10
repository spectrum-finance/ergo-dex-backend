package org.ergoplatform.dex.domain

sealed trait ValueUnits

object ValueUnits {

  case object USD extends ValueUnits
}
