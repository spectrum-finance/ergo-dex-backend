package org.ergoplatform.dex.index.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.cache.RedisConfig
import org.ergoplatform.common.db.PgConfig
import org.ergoplatform.common.streaming.CommitPolicy
import org.ergoplatform.dex.configs._
import org.ergoplatform.dex.tracker.configs.TrackerConfig
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{ClassyOptics, promote}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote commitPolicy: CommitPolicy,
  cfmmOrdersConsumer: ConsumerConfig,
  cfmmOrdersProducer: ProducerConfig,
  cfmmPoolsProducer: ProducerConfig,
  @promote db: PgConfig,
  @promote kafka: KafkaConfig,
  @promote protocol: ProtocolConfig,
  @promote network: NetworkConfig,
  @promote tracker: TrackerConfig,
  @promote monetary: MonetaryConfig,
  redis: RedisConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
