package org.ergoplatform.dex.executor.amm.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.sttp.instances._
import sttp.model.Uri
import tofu.Context
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class ResolverConfig(uri: Uri)

object ResolverConfig extends Context.Companion[ResolverConfig]
