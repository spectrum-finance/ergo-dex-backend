package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class KafkaConfig(bootstrapServers: List[String])

object KafkaConfig extends Context.Companion[KafkaConfig]
