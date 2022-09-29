package org.ergoplatform.dex.index.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.cache.RedisConfig
import org.ergoplatform.common.db.PgConfig
import org.ergoplatform.common.streaming.CommitPolicy
import org.ergoplatform.dex.configs._
import org.ergoplatform.dex.tracker.configs.{BlockTrackerConfig, LedgerTrackingConfig, TxTrackerConfig}
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{promote, ClassyOptics}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote commitPolicy: CommitPolicy,
  consumers: Consumers,
  producers: Producers,
  @promote db: PgConfig,
  @promote kafka: KafkaConfig,
  @promote protocol: ProtocolConfig,
  @promote network: NetworkConfig,
  @promote utxoTracker: LedgerTrackingConfig,
  @promote txTracker: TxTrackerConfig,
  @promote blockTracker: BlockTrackerConfig,
  redis: RedisConfig,
  poolsIndexing: PoolsIndexingConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
