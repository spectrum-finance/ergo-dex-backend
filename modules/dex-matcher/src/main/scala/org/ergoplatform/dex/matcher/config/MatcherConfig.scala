package org.ergoplatform.dex.matcher.config

import scala.concurrent.duration.FiniteDuration

final case class MatcherConfig(batchSize: Int, interval: FiniteDuration)
