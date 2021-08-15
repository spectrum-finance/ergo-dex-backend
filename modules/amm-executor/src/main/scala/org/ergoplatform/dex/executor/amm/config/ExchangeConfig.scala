package org.ergoplatform.dex.executor.amm.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.ergo.Address
import tofu.Context
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class ExchangeConfig(rewardAddress: Address)

object ExchangeConfig extends Context.Companion[ExchangeConfig]
