package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.{Context, WithContext}
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class KafkaConfig(bootstrapServers: List[String])

object KafkaConfig extends WithContext.Companion[KafkaConfig]
