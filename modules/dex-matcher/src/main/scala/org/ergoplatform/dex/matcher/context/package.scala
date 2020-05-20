package org.ergoplatform.dex.matcher

import org.ergoplatform.dex.domain.{Match, Order}
import org.ergoplatform.dex.streaming.{Consumer, Producer}
import tofu.optics.Extract
import tofu.{Context, HasContext}

package object context {

  type HasMatcherContext[F[_]] = F HasContext MatcherContext[F]

  @inline def askConsumer[F[_]: HasMatcherContext](
    implicit lens: MatcherContext[F] Extract Consumer[F, Order]
  ): F[Consumer[F, Order]] =
    Context[F].extract(lens).context

  @inline def askProducer[F[_]: HasMatcherContext](
    implicit lens: MatcherContext[F] Extract Producer[F, Match]
  ): F[Producer[F, Match]] =
    Context[F].extract(lens).context
}
