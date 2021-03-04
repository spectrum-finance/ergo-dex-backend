package org.ergoplatform.dex.markets.modules

import org.ergoplatform.dex.clients.explorer.models.Transaction
import org.ergoplatform.dex.markets.models.Trade
import org.ergoplatform.dex.protocol.OrderScripts

trait Trades[F[_]] {

  def extract(tx: Transaction): F[Option[Trade]]
}

object Trades {

  final class Live[F[_]](implicit scripts: OrderScripts) extends Trades[F] {
    def extract(tx: Transaction): F[Option[Trade]] = ???
  }
}
