package org.ergoplatform.dex.matcher.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.{Context, WithContext}

import scala.concurrent.duration.FiniteDuration

@derive(pureconfigReader)
final case class MatcherConfig(batchSize: Int, batchInterval: FiniteDuration)

object MatcherConfig extends WithContext.Companion[MatcherConfig]
