package org.ergoplatform.dex.markets.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.cache.RedisConfig
import org.ergoplatform.common.db.PgConfig
import org.ergoplatform.common.http.config.HttpConfig
import org.ergoplatform.dex.configs.{ConfigBundleCompanion, KafkaConfig, NetworkConfig}
import org.ergoplatform.graphite.GraphiteSettings
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{ClassyOptics, promote}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote db: PgConfig,
  @promote network: NetworkConfig,
  @promote kafka: KafkaConfig,
  http: HttpConfig,
  redis: RedisConfig,
  tokens: TokenFetcherConfig,
  request: RequestConfig,
  consumers: Consumers,
  graphite: GraphiteSettings,
  graphitePathPrefix: String
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
