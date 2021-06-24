package org.ergoplatform.dex.resolver.config

import cats.syntax.either._
import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.http.sttpInstances._
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import sttp.model.Uri

@derive(pureconfigReader)
final case class ResolverConfig(uri: Uri)
