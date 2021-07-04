package org.ergoplatform.common.http

import cats.data.EitherT

package object syntax {

  implicit def toAdaptThrowableOps[F[_], G[_, _], E, A](fa: F[A])(implicit
    A: AdaptThrowable[F, G, E]
  ): AdaptThrowableOps[F, G, E, A] =
    new AdaptThrowableOps(fa)

  implicit def toRoutesOps[F[_], A](fa: EitherT[F, HttpError, Option[A]]): RoutesOps[F, A] =
    new RoutesOps(fa)
}
