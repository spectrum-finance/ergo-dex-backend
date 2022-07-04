package org.ergoplatform.dex.markets.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs.ConsumerConfig

@derive(pureconfigReader)
final case class Consumers(
  blocks: ConsumerConfig
)
