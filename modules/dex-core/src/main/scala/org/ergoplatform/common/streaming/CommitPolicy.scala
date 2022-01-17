package org.ergoplatform.common.streaming
import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.{Context, WithContext}
import tofu.logging.derivation.loggable

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader, loggable)
final case class CommitPolicy(maxBatchSize: Int, commitTimeout: FiniteDuration)

object CommitPolicy extends WithContext.Companion[CommitPolicy]
