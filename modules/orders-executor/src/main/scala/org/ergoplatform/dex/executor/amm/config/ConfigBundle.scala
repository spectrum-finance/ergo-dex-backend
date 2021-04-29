package org.ergoplatform.dex.executor.amm.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.configs.{ConfigBundleCompanion, ConsumerConfig, NetworkConfig, ProducerConfig, ProtocolConfig}
import org.ergoplatform.dex.streaming.CommitPolicy
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{ClassyOptics, promote}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote commitPolicy: CommitPolicy,
  @promote exchange: ExchangeConfig,
  @promote protocol: ProtocolConfig,
  @promote consumer: ConsumerConfig,
  @promote producer: ProducerConfig,
  @promote network: NetworkConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
