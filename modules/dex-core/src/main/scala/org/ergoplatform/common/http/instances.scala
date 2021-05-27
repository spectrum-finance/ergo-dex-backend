package org.ergoplatform.common.http

import cats.tagless.InvariantK
import cats.~>
import sttp.capabilities
import sttp.client3.{Request, Response, SttpBackend}
import sttp.monad.MonadError

object instances {

  implicit def sttpInvK[P]: InvariantK[SttpBackend[*[_], P]] =
    new InvariantK[SttpBackend[*[_], P]] {

      def imapK[F[_], G[_]](af: SttpBackend[F, P])(fk: F ~> G)(gK: G ~> F): SttpBackend[G, P] =
        new SttpBackend[G, P] {

          def send[T, R >: P with capabilities.Effect[G]](request: Request[T, R]): G[Response[T]] =
            fk(af.send(request.asInstanceOf[Request[T, P with capabilities.Effect[F]]]))
          def close(): G[Unit]             = fk(af.close())
          def responseMonad: MonadError[G] = meInvK.imapK(af.responseMonad)(fk)(gK)
        }
    }

  implicit def meInvK: InvariantK[MonadError] =
    new InvariantK[MonadError] {

      def imapK[F[_], G[_]](af: MonadError[F])(fk: F ~> G)(gK: G ~> F): MonadError[G] =
        new MonadError[G] {
          def unit[T](t: T): G[T]                            = fk(af.unit(t))
          def map[T, T2](fa: G[T])(f: T => T2): G[T2]        = fk(af.map(gK(fa))(f))
          def flatMap[T, T2](fa: G[T])(f: T => G[T2]): G[T2] = fk(af.flatMap(gK(fa))(t => gK(f(t))))
          def error[T](t: Throwable): G[T]                   = fk(af.error(t))
          def ensure[T](f: G[T], e: => G[Unit]): G[T]        = fk(af.ensure(gK(f), gK(e)))

          protected def handleWrappedError[T](rt: G[T])(h: PartialFunction[Throwable, G[T]]): G[T] =
            fk(af.handleError(gK(rt))({ case t => gK(h(t)) }))
        }
    }
}