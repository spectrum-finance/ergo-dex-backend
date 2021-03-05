package org.ergoplatform.dex.markets.modules

import cats.Monad
import cats.instances.list._
import cats.syntax.traverse._
import org.ergoplatform.dex.SErgoTree
import org.ergoplatform.dex.clients.explorer.models.{Input, Transaction}
import org.ergoplatform.dex.markets.models.Trade
import org.ergoplatform.dex.protocol.OrderScripts
import tofu.syntax.monadic._
import tofu.syntax.foption._

trait Trades[F[_]] {

  def extract(tx: Transaction): F[List[Trade]]
}

object Trades {

  final class Live[F[_]: Monad](implicit scripts: OrderScripts[F]) extends Trades[F] {

    def extract(tx: Transaction): F[List[Trade]] = {
      val collectInputs: (SErgoTree => F[Boolean]) => F[List[Input]] =
        p => tx.inputs.traverse(i => p(i.ergoTree)).map(_.zip(tx.inputs).collect { case (true, in) => in })
      for {
        asksIn <- collectInputs(scripts.isAsk)
        asks <- asksIn
                  .traverse(in => scripts.parseAsk(in.ergoTree, in.additionalRegisters).mapIn(in -> _))
                  .map(_.flatten)
        bidsIn <- collectInputs(scripts.isBid)
        bids <- bidsIn
                  .traverse(in => scripts.parseBid(in.ergoTree, in.additionalRegisters).mapIn(in -> _))
                  .map(_.flatten)
      } yield ???
    }
  }
}
