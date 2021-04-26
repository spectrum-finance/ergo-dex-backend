package org.ergoplatform.dex.tracker.processes

trait AmmTracker[F[_]] {

  def run: F[Unit]
}
