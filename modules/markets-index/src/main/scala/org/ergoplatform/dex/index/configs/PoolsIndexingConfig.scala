package org.ergoplatform.dex.index.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context
import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader)
case class PoolsIndexingConfig(limitRetries: Int, retryDelay: FiniteDuration)

object PoolsIndexingConfig extends Context.Companion[PoolsIndexingConfig]
