package org.ergoplatform.dex.index.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.db.PgConfig
import org.ergoplatform.dex.configs.ConfigBundleCompanion
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{promote, ClassyOptics}

@derive(pureconfigReader)
@ClassyOptics
final case class ConfigBundle(
  @promote db: PgConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle] with ConfigBundleCompanion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty
}
