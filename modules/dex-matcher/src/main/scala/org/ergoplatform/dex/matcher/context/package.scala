package org.ergoplatform.dex.matcher

import org.ergoplatform.dex.PairId
import org.ergoplatform.dex.domain.models.Match.AnyMatch
import org.ergoplatform.dex.domain.models.Order.AnyOrder
import org.ergoplatform.dex.streaming.{Consumer, Producer}
import tofu.optics.Extract
import tofu.{Context, HasContext, HasProvide}

package object context {

  type HasMatcherContext[F[_]]      = F HasContext MatcherContext[F]
  type HasProvidePairId[F[_], G[_]] = HasProvide[F, G, PairId]
  type HasPairId[F[_]]              = F HasContext PairId

  @inline def askConsumer[F[_]: HasMatcherContext](
    implicit lens: MatcherContext[F] Extract Consumer[F, AnyOrder]
  ): F[Consumer[F, AnyOrder]] =
    Context[F].extract(lens).context

  @inline def askProducer[F[_]: HasMatcherContext](
    implicit lens: MatcherContext[F] Extract Producer[F, AnyMatch]
  ): F[Producer[F, AnyMatch]] =
    Context[F].extract(lens).context
}
