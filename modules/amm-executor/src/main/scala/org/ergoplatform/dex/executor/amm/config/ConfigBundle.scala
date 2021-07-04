package org.ergoplatform.dex.executor.amm.config

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
  @promote exchange: ExchangeConfig,
  @promote execution: ExecutionConfig,
  @promote protocol: ProtocolConfig,
  @promote consumer: ConsumerConfig,
  @promote kafka: KafkaConfig,
  @promote explorer: ExplorerConfig,
  @promote resolver: ResolverConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
