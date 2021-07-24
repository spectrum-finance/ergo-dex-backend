package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.sttp.instances._
import sttp.model.Uri
import tofu.Context

@derive(pureconfigReader)
final case class ExplorerConfig(uri: Uri)

object ExplorerConfig extends Context.Companion[ExplorerConfig]
