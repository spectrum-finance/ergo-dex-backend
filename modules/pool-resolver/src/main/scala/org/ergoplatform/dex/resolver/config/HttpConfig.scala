package org.ergoplatform.dex.resolver.config

import derevo.derive
import derevo.pureconfig.pureconfigReader

@derive(pureconfigReader)
final case class HttpConfig(host: String, port: Int)
