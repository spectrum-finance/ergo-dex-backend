package org.ergoplatform.ergo.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import tofu.logging.derivation.loggable
import org.ergoplatform.ergo.services.explorer.models.{Output => ExplorerOutput}

@derive(encoder, decoder, loggable)
final case class SettledOutput(output: Output, gix: Long, settlementHeight: Int)

object SettledOutput {
  def fromExplorer(o: ExplorerOutput): SettledOutput =
    SettledOutput(Output.fromExplorer(o), o.globalIndex, o.settlementHeight)
}
