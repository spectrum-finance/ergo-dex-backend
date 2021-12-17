package org.ergoplatform.common.http

import cats.MonadError
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.common.http.AdaptThrowable.AdaptThrowableEitherT

@derive(encoder, decoder)
sealed trait HttpError

object HttpError {
  @derive(encoder, decoder)
  final case class NotFound(what: String) extends HttpError
  @derive(encoder, decoder)
  final case class Unauthorized(realm: String) extends HttpError
  @derive(encoder, decoder)
  final case class Unknown(code: Int, msg: String) extends HttpError
  @derive(encoder, decoder)
  case object NoContent extends HttpError

  implicit def adaptThrowable[F[_]](implicit
    F: MonadError[F, Throwable]
  ): AdaptThrowableEitherT[F, HttpError] =
    new AdaptThrowableEitherT[F, HttpError] {

      final def adapter: Throwable => F[HttpError] = e => F.pure(Unknown(500, e.getMessage))
    }
}
