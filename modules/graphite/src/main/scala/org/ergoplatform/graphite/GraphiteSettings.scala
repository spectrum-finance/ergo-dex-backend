package org.ergoplatform.graphite

import derevo.derive
import derevo.pureconfig.pureconfigReader

@derive(pureconfigReader)
final case class GraphiteSettings(
  host: String,
  port: Int,
  batchSize: Int
)
