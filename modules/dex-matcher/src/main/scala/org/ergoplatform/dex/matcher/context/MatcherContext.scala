package org.ergoplatform.dex.matcher.context

import org.ergoplatform.dex.domain.models.Match
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.streaming.{Consumer, Producer}
import tofu.optics.macros.{ClassyOptics, promote}

@ClassyOptics
final case class MatcherContext[F[_]](
  @promote consumer: Consumer[F, AnyOrder],
  @promote producer: Producer[F, Match]
)
