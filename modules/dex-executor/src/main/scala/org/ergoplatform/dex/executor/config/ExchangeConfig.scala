package org.ergoplatform.dex.executor.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.Address

@derive(pureconfigReader)
final case class ExchangeConfig(rewardAddress: Address)
