package org.ergoplatform.dex.resolver.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.streaming.CommitPolicy
import org.ergoplatform.dex.configs._
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{promote, ClassyOptics}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote commitPolicy: CommitPolicy,
  ordersProducer: ProducerConfig,
  cfmmProducer: ProducerConfig,
  @promote kafka: KafkaConfig,
  @promote protocol: ProtocolConfig,
  @promote network: NetworkConfig,
  @promote tracker: TrackerConfig,
  @promote fees: Fees
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
