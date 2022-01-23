package org.ergoplatform.dex.tracker.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs.ProducerConfig

@derive(pureconfigReader)
final case class Publishers(
  ammOrders: ProducerConfig,
  ammPools: ProducerConfig,
  lqLocks: ProducerConfig
)
