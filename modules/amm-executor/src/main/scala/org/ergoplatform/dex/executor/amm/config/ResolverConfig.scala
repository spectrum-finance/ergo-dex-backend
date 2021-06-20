package org.ergoplatform.dex.executor.amm.config

import sttp.model.Uri
import tofu.Context

final case class ResolverConfig(uri: Uri)

object ResolverConfig extends Context.Companion[ResolverConfig]
