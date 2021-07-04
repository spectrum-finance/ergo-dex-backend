package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context

@derive(pureconfigReader)
final case class ExecutionConfig(minerFee: Long)

object ExecutionConfig extends Context.Companion[ExecutionConfig]
