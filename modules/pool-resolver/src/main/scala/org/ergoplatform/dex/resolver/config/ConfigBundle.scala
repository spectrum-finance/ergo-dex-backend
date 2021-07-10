package org.ergoplatform.dex.resolver.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.cache.RedisConfig
import org.ergoplatform.dex.configs.{ConfigBundleCompanion, ConsumerConfig, KafkaConfig}
import tofu.optics.macros.{ClassyOptics, promote}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  http: HttpConfig,
  @promote kafka: KafkaConfig,
  @promote consumer: ConsumerConfig,
  redis: RedisConfig
)

object ConfigBundle extends ConfigBundleCompanion[ConfigBundle]
