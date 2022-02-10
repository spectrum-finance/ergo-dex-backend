package org.ergoplatform.dex.tracker.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader)
final case class LedgerTrackingConfig(
  initialOffset: Long,
  batchSize: Int,
  retryDelay: FiniteDuration
)

object LedgerTrackingConfig extends Context.Companion[LedgerTrackingConfig]
