package org.ergoplatform.dex.executor.amm.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.sttp.instances._
import sttp.model.Uri
import tofu.Context

@derive(pureconfigReader)
final case class ResolverConfig(uri: Uri)

object ResolverConfig extends Context.Companion[ResolverConfig]
