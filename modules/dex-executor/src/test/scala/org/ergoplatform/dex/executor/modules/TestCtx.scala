package org.ergoplatform.dex.executor.modules

import org.ergoplatform.dex.executor.config.ConfigBundle
import org.ergoplatform.dex.executor.context.BlockchainContext
import tofu.Context
import tofu.optics.macros.{promote, ClassyOptics}

@ClassyOptics
final case class TestCtx(@promote configs: ConfigBundle, @promote blockchain: BlockchainContext)
object TestCtx extends Context.Companion[TestCtx]
