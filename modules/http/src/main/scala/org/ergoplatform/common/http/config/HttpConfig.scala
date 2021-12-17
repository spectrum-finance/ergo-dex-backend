package org.ergoplatform.common.http.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class HttpConfig(host: String, port: Int)
