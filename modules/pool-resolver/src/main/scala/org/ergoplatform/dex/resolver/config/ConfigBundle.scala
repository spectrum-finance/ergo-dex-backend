package org.ergoplatform.dex.resolver.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs.{ConfigBundleCompanion, ConsumerConfig, KafkaConfig}
import tofu.optics.macros.{promote, ClassyOptics}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  http: HttpConfig,
  @promote kafka: KafkaConfig,
  @promote consumer: ConsumerConfig
)

object ConfigBundle extends ConfigBundleCompanion[ConfigBundle]
