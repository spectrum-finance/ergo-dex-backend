package org.ergoplatform.common.cache

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class RedisConfig(uri: String)

object RedisConfig extends Context.Companion[RedisConfig]
