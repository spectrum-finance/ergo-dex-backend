package org.ergoplatform.dex.markets.modules

import cats.Id
import org.ergoplatform.dex.protocol.ContractType
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec

class FillsSpec extends AnyPropSpec with should.Matchers {

  property("Trades extraction") {
    val trades = Fills.instance[Id, ContractType.LimitOrder]
    println(trades.extract(samples.TradeTx))
  }
}
