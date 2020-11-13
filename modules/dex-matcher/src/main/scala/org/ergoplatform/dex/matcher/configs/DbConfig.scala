package org.ergoplatform.dex.matcher.configs

import scala.concurrent.duration.FiniteDuration

final case class DbConfig(
  url: String,
  user: String,
  pass: String,
  connectionTimeout: FiniteDuration,
  minConnections: Int,
  maxConnections: Int
)
