package org.ergoplatform.dex.protocol

import io.circe.Json
import org.ergoplatform.dex.SErgoTree

trait OrderScripts[F[_]] {

  def isAsk(ergoTree: SErgoTree): F[Boolean]

  def isBid(ergoTree: SErgoTree): F[Boolean]

  def parseAsk(ergoTree: SErgoTree, registers: Json): F[Option[AskParams]]

  def parseBid(ergoTree: SErgoTree, registers: Json): F[Option[BidParams]]
}
