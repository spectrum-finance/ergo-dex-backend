package org.ergoplatform.common.cache

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class RedisConfig(uri: String)
