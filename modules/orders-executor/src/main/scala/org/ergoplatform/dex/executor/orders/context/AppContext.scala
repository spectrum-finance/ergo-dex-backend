package org.ergoplatform.dex.executor.orders.context

import io.estatico.newtype.ops._
import org.ergoplatform.common.TraceId
import org.ergoplatform.dex.executor.orders.config.ConfigBundle
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
    AppContext(configs, BlockchainContext.empty, "<Root>".coerce[TraceId])

  implicit val loggable: Loggable[AppContext] = Loggable.stringValue.contramap[AppContext](_.traceId.value)
}
