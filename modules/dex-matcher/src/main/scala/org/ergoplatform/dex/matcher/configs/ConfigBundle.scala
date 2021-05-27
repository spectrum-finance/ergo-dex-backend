package org.ergoplatform.dex.matcher.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs.{ConfigBundleCompanion, ConsumerConfig, DbConfig, KafkaConfig, ProducerConfig}
import org.ergoplatform.common.streaming.CommitPolicy
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{ClassyOptics, promote}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote matcher: MatcherConfig,
  @promote db: DbConfig,
  @promote commitPolicy: CommitPolicy,
  @promote consumer: ConsumerConfig,
  producer: ProducerConfig,
  @promote kafka: KafkaConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
