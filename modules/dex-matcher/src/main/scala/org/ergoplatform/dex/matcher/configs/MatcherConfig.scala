package org.ergoplatform.dex.matcher.configs

import scala.concurrent.duration.FiniteDuration

final case class MatcherConfig(batchSize: Int, interval: FiniteDuration)
