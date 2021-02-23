package org.ergoplatform.dex.markets.processes

trait MarketsIndexer[F[_]] {

  def run: F[Unit]
}
