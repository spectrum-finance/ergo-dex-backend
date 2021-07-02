package org.ergoplatform.dex.markets.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.db.PgConfig
import org.ergoplatform.dex.configs.{ConfigBundleCompanion, ExplorerConfig}
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{promote, ClassyOptics}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote indexer: IndexerConfig,
  @promote pg: PgConfig,
  @promote network: ExplorerConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
