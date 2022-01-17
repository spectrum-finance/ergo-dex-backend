package org.ergoplatform.dex.tracker.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.{Context, WithContext}

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader)
final case class TxTrackerConfig(
  initialOffset: Long,
  batchSize: Int,
  retryDelay: FiniteDuration
)

object TxTrackerConfig extends WithContext.Companion[TxTrackerConfig]
