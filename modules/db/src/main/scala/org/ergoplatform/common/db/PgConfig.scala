package org.ergoplatform.common.db

import derevo.derive
import derevo.pureconfig.pureconfigReader

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader)
final case class PgConfig(
  url: String,
  user: String,
  pass: String,
  connectionTimeout: FiniteDuration,
  minConnections: Int,
  maxConnections: Int
)
