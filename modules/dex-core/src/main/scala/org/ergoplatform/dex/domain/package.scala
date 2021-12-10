package org.ergoplatform.dex

import derevo.derive
import doobie.{Get, Put}
import io.estatico.newtype.macros.newtype
import org.ergoplatform.common.HexString
import org.ergoplatform.ergo.TokenId
import tofu.logging.derivation.loggable

package object domain {

  @derive(loggable)
  final case class PairId(quoteId: TokenId, baseId: TokenId)

  @newtype
  final case class Ticker(value: String)

  object Ticker {

    implicit val get: Get[Ticker] = deriving
    implicit val put: Put[Ticker] = deriving
  }
}
