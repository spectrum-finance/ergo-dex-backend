package org.ergoplatform.dex.markets.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader)
final case class IndexerConfig(scanInterval: FiniteDuration, batchSize: Int)

object IndexerConfig extends Context.Companion[IndexerConfig]
