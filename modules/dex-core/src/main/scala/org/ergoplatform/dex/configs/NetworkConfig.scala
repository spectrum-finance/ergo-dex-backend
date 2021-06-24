package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.http.sttpInstances._
import sttp.model.Uri

@derive(pureconfigReader)
final case class NetworkConfig(explorerUri: Uri)
