package org.ergoplatform.alg

import cats.FlatMap

trait ConstrainedEmbed[U[_[_]], C[_[_]]] {
  def embed[F[_]: FlatMap: C](ft: F[U[F]]): U[F]
}
