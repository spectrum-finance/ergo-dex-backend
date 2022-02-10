package org.ergoplatform.dex.resolver.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs.ConsumerConfig
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class Consumers(confirmedPools: ConsumerConfig, unconfirmedPools: ConsumerConfig)
