package org.ergoplatform.common.http.syntax

import org.ergoplatform.common.http.AdaptThrowable

final class AdaptThrowableOps[F[_], G[_, _], E, A](fa: F[A])(
  implicit A: AdaptThrowable[F, G, E]
) {

  def adaptThrowable: G[E, A] = A.adaptThrowable(fa)
}