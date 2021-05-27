package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context

@derive(pureconfigReader)
final case class KafkaConfig(bootstrapServers: List[String])

object KafkaConfig extends Context.Companion[KafkaConfig]
