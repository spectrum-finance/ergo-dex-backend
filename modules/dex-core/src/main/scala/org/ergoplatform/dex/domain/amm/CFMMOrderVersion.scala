package org.ergoplatform.dex.domain.amm

sealed trait CFMMOrderVersion

object CFMMOrderVersion {
  sealed trait V0 extends CFMMOrderVersion
  sealed trait V1 extends CFMMOrderVersion
}