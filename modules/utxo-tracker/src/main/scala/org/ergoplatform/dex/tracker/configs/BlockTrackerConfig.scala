package org.ergoplatform.dex.tracker.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader)
final case class BlockTrackerConfig(
  initialOffset: Long,
  batchSize: Int,
  retryDelay: FiniteDuration
)

object BlockTrackerConfig extends Context.Companion[BlockTrackerConfig]
