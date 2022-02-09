package org.ergoplatform.dex.tracker.processes

import derevo.derive
import tofu.higherKind.derived.representableK

@derive(representableK)
trait UtxoTracker[F[_]] {

  def run: F[Unit]
}
