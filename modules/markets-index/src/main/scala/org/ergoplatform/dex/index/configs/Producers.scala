package org.ergoplatform.dex.index.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs.ProducerConfig

@derive(pureconfigReader)
final case class Producers(
  cfmmHistory: ProducerConfig,
  cfmmPools: ProducerConfig
)
