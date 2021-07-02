package org.ergoplatform.dex.executor.orders.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs._
import org.ergoplatform.common.streaming.CommitPolicy
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{promote, ClassyOptics}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote commitPolicy: CommitPolicy,
  @promote exchange: ExchangeConfig,
  @promote protocol: ProtocolConfig,
  @promote consumer: ConsumerConfig,
  producer: ProducerConfig,
  @promote kafka: KafkaConfig,
  @promote network: ExplorerConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
