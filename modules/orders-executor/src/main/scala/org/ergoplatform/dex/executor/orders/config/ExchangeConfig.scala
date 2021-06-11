package org.ergoplatform.dex.executor.orders.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.ergo.Address
import tofu.Context

@derive(pureconfigReader)
final case class ExchangeConfig(rewardAddress: Address, executionFeeAmount: Long)

object ExchangeConfig extends Context.Companion[ExchangeConfig]
