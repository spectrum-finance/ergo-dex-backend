package org.ergoplatform.dex.executor.amm.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs.ConsumerConfig
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class Consumers(
  confirmedOrders: ConsumerConfig,
  unconfirmedOrders: ConsumerConfig,
  ordersRetry: ConsumerConfig
)
