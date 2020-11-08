package org.ergoplatform.dex.executor.config

import tofu.Context
import tofu.optics.macros.{ClassyOptics, promote}

@ClassyOptics
final case class ConfigBundle(
  @promote exchange: ExchangeConfig,
  @promote protocol: ProtocolConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle]
