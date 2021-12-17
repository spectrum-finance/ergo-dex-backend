package org.ergoplatform.common.http.syntax

import cats.Applicative
import cats.syntax.either._
import org.ergoplatform.common.http.HttpError
import tofu.Catches
import tofu.syntax.handle._
import tofu.syntax.monadic._

final class ServiceOps[F[_], A](protected val fa: F[A]) extends AnyVal {

  def eject(implicit F: Applicative[F], C: Catches[F]): F[Either[HttpError, A]] =
    fa.map(_.asRight[HttpError]).handle[Throwable](e => HttpError.Unknown(500, e.getMessage).asLeft)
}
