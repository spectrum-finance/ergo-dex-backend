package org.ergoplatform.dex.matcher.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader)
final case class MatcherConfig(batchSize: Int, interval: FiniteDuration)
