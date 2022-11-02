package org.ergoplatform.dex.protocol.amm

sealed trait ParserType

object ParserType {
  sealed trait Default extends ParserType
  sealed trait MultiAddress extends ParserType

  type Any = ParserType
}