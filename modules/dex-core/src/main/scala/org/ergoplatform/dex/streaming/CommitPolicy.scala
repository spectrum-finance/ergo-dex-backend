package org.ergoplatform.dex.streaming
import scala.concurrent.duration.FiniteDuration

final case class CommitPolicy(maxBatchSize: Int, commitTimeout: FiniteDuration)
