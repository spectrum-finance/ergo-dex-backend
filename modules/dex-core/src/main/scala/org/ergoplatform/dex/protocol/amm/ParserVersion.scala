package org.ergoplatform.dex.protocol.amm

sealed trait ParserVersion

object ParserVersion {
  sealed trait V1 extends ParserVersion

  sealed trait V2 extends ParserVersion

  sealed trait V3 extends ParserVersion

  type Any = ParserVersion
}
