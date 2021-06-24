package org.ergoplatform.common.http.syntax

import cats.Monad
import cats.data.EitherT
import cats.syntax.applicative._
import org.ergoplatform.common.http.HttpError

final class RoutesOps[F[_], A](val fa: EitherT[F, HttpError, Option[A]]) extends AnyVal {

  def orNotFound(what: String)(implicit M: Monad[F]): EitherT[F, HttpError, A] =
    fa.flatMap(
      _.fold[EitherT[F, HttpError, A]](EitherT.left((HttpError.NotFound(what): HttpError).pure[F]))(
        EitherT.pure[F, HttpError](_)
      )
    )
}
