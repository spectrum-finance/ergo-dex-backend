package org.ergoplatform.dex.resolver

import derevo.derive
import io.estatico.newtype.ops._
import org.ergoplatform.common.TraceId
import org.ergoplatform.dex.resolver.config.ConfigBundle
import tofu.Context
import tofu.logging.derivation.{hidden, loggable}
import tofu.optics.macros.{ClassyOptics, promote}

@ClassyOptics
@derive(loggable)
final case class AppContext(
  @promote @hidden configs: ConfigBundle,
  @promote traceId: TraceId
)

object AppContext extends Context.Companion[AppContext] {

  def init(configs: ConfigBundle): AppContext =
    AppContext(configs, "<Root>".coerce[TraceId])
}
