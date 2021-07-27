package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class ExecutionConfig(minerFee: Long, minDexFee: Long, minBoxValue: Long)

object ExecutionConfig extends Context.Companion[ExecutionConfig]
