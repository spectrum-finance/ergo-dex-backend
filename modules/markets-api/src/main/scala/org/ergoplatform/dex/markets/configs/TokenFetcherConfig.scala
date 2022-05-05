package org.ergoplatform.dex.markets.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
case class TokenFetcherConfig(filePath: String)

object TokenFetcherConfig extends Context.Companion[TokenFetcherConfig]
