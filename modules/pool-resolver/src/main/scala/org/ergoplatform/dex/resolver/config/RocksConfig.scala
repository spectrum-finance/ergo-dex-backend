package org.ergoplatform.dex.resolver.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.{Context, WithContext}
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class RocksConfig(path: String)

object RocksConfig extends WithContext.Companion[RocksConfig]
