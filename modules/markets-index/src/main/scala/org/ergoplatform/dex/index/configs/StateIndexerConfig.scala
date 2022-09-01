package org.ergoplatform.dex.index.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader

@derive(pureconfigReader)
final case class StateIndexerConfig (tokensIds: List[String])
