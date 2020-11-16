package org.ergoplatform.dex.streaming
import derevo.derive
import derevo.pureconfig.pureconfigReader

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader)
final case class CommitPolicy(maxBatchSize: Int, commitTimeout: FiniteDuration)
