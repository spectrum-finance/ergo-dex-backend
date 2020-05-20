package org.ergoplatform.dex.matcher.context

import org.ergoplatform.dex.domain.{Match, Order}
import org.ergoplatform.dex.streaming.{Consumer, Producer}
import tofu.optics.macros.{promote, ClassyOptics}

@ClassyOptics
final case class MatcherContext[F[_]](
  @promote consumer: Consumer[F, Order],
  @promote producer: Producer[F, Match]
)
