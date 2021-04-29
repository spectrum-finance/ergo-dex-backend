package org.ergoplatform.dex.executor.amm.context

import io.estatico.newtype.ops._
import org.ergoplatform.dex.TraceId
import org.ergoplatform.dex.executor.amm.config.ConfigBundle
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{promote, ClassyOptics}

@ClassyOptics
final case class AppContext(
  @promote configs: ConfigBundle,
  @promote blockchain: BlockchainContext,
  @promote traceId: TraceId
)

object AppContext extends Context.Companion[AppContext] {

  def init(configs: ConfigBundle): AppContext =
    AppContext(configs, BlockchainContext.empty, "<Initial>".coerce[TraceId])

  implicit val loggable: Loggable[AppContext] = Loggable.stringValue.contramap[AppContext](_.traceId.value)
}
