package org.ergoplatform.dex.matcher.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader)
final case class DbConfig(
  url: String,
  user: String,
  pass: String,
  connectionTimeout: FiniteDuration,
  minConnections: Int,
  maxConnections: Int
)
