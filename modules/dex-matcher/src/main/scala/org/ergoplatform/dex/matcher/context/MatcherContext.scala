package org.ergoplatform.dex.matcher.context

import org.ergoplatform.dex.domain.Order.AnyOrder
import org.ergoplatform.dex.domain.{Match, Order}
import org.ergoplatform.dex.streaming.{Consumer, Producer}
import tofu.optics.macros.{promote, ClassyOptics}

@ClassyOptics
final case class MatcherContext[F[_]](
  @promote consumer: Consumer[F, AnyOrder],
  @promote producer: Producer[F, Match]
)
