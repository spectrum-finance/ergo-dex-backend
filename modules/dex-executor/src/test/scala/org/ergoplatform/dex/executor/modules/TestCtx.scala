package org.ergoplatform.dex.executor.modules

import org.ergoplatform.dex.configs.ProtocolConfig
import org.ergoplatform.dex.executor.config.ExchangeConfig
import org.ergoplatform.dex.executor.context.BlockchainContext
import tofu.Context
import tofu.optics.macros.{ClassyOptics, promote}

@ClassyOptics
final case class TestCtx(
  @promote exchange: ExchangeConfig,
  @promote protocol: ProtocolConfig,
  @promote blockchain: BlockchainContext
)
object TestCtx extends Context.Companion[TestCtx]