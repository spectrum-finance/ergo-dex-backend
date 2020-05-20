package org.ergoplatform.dex.matcher

import org.ergoplatform.dex.domain.Match
import org.ergoplatform.dex.domain.Order.AnyOrder
import org.ergoplatform.dex.streaming.{Consumer, Producer}
import tofu.optics.Extract
import tofu.{Context, HasContext}

package object context {

  type HasMatcherContext[F[_]] = F HasContext MatcherContext[F]

  @inline def askConsumer[F[_]: HasMatcherContext](
    implicit lens: MatcherContext[F] Extract Consumer[F, AnyOrder]
  ): F[Consumer[F, AnyOrder]] =
    Context[F].extract(lens).context

  @inline def askProducer[F[_]: HasMatcherContext](
    implicit lens: MatcherContext[F] Extract Producer[F, Match]
  ): F[Producer[F, Match]] =
    Context[F].extract(lens).context
}
