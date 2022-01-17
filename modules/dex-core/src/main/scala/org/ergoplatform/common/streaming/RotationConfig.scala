package org.ergoplatform.common.streaming

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.{Context, WithContext}
import tofu.logging.derivation.loggable

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader, loggable)
final case class RotationConfig(retryDelay: FiniteDuration)

object RotationConfig extends WithContext.Companion[RotationConfig]
