package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader

@derive(pureconfigReader)
final case class NetworkConfig(explorerUri: String)
