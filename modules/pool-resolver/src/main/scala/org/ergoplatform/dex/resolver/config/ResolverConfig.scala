package org.ergoplatform.dex.resolver.config

import cats.syntax.either._
import derevo.derive
import derevo.pureconfig.pureconfigReader
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import sttp.model.Uri

@derive(pureconfigReader)
final case class ResolverConfig(uri: Uri)

object ResolverConfig {

  implicit val configReader: ConfigReader[Uri] =
    ConfigReader.fromString(s => Uri.parse(s).leftMap(r => CannotConvert(s, "Uri", r)))
}
