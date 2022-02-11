package org.ergoplatform.dex.executor.amm.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs.ProducerConfig
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class Producers(ordersRetry: ProducerConfig)
