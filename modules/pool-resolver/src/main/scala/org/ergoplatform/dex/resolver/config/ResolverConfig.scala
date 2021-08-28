package org.ergoplatform.dex.resolver.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.sttp.instances._
import sttp.model.Uri

@derive(pureconfigReader)
final case class ResolverConfig(uri: Uri)
