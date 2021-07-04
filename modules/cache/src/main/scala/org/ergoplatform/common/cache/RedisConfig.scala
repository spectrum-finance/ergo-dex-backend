package org.ergoplatform.common.cache

import derevo.derive
import derevo.pureconfig.pureconfigReader

@derive(pureconfigReader)
final case class RedisConfig(uri: String)
