package org.ergoplatform.dex.tracker.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs.ProducerConfig

@derive(pureconfigReader)
final case class Producers(
  confirmedAmmOrders: ProducerConfig,
  unconfirmedAmmOrders: ProducerConfig,
  confirmedAmmPools: ProducerConfig,
  unconfirmedAmmPools: ProducerConfig
)
