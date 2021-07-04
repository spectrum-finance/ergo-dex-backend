package org.ergoplatform.dex.tracker.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader)
final case class TrackerConfig(
  initialOffset: Long,
  batchSize: Int,
  retryDelay: FiniteDuration
)

object TrackerConfig extends Context.Companion[TrackerConfig]
