package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.sttp.instances._
import sttp.model.Uri
import tofu.Context
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class NetworkConfig(explorerUri: Uri, nodeUri: Uri)

object NetworkConfig extends Context.Companion[NetworkConfig]
