package org.ergoplatform.dex.executor.amm.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.streaming.RotationConfig
import org.ergoplatform.dex.configs._
import tofu.Context
import tofu.logging.derivation.loggable
import tofu.optics.macros.{promote, ClassyOptics}

@derive(pureconfigReader, loggable)
@ClassyOptics
final case class ConfigBundle(
  @promote rotation: RotationConfig,
  @promote exchange: ExchangeConfig,
  @promote execution: ExecutionConfig,
  @promote protocol: ProtocolConfig,
  @promote ordersConsumer: ConsumerConfig,
  ordersRotation: ProducerConfig,
  @promote kafka: KafkaConfig,
  @promote explorer: ExplorerConfig,
  @promote resolver: ResolverConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle]
