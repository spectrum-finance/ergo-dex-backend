package org.ergoplatform.dex.index.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs.ConsumerConfig

@derive(pureconfigReader)
final case class Consumers(
  cfmmHistory: ConsumerConfig,
  cfmmPools: ConsumerConfig,
  lqLocks: ConsumerConfig,
  blocks: ConsumerConfig,
  txns: ConsumerConfig
)
