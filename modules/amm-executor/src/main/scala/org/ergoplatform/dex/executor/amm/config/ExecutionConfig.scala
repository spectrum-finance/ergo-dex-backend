package org.ergoplatform.dex.executor.amm.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context
import tofu.logging.derivation.loggable

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader, loggable)
final case class ExecutionConfig(orderLifetime: FiniteDuration)

object ExecutionConfig extends Context.Companion[ExecutionConfig]