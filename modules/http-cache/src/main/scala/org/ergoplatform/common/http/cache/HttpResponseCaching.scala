package org.ergoplatform.common.http.cache

import cats.data.OptionT
import cats.effect.Sync
import cats.{FlatMap, Monad}
import org.ergoplatform.common.cache.Cache
import org.ergoplatform.common.http.cache.models.CachedResponse
import org.ergoplatform.common.http.cache.models.CachedResponse._
import org.ergoplatform.common.http.cache.types.Hash32
import org.http4s._
import scodec.bits.ByteVector
import scorex.crypto.hash.{Blake2b256, Digest32}
import tofu.logging.Logs
import tofu.logging.derivation.loggable.generate
import tofu.syntax.embed._
import tofu.syntax.monadic.{TofuFlatMapOps, TofuFunctorOps}

trait HttpResponseCaching[F[_]] {

  def process(req: Request[F]): F[Option[Response[F]]]

  def saveResponse(reqHash: Hash32, resp: Response[F]): F[Unit]

  def invalidate: F[Unit]
}

object HttpResponseCaching {

  def fromResponse[F[_]: Sync](resp: Response[F]): F[CachedResponse] =
    resp.body.compile.to(ByteVector).map { body =>
      CachedResponse(
        resp.status,
        resp.httpVersion,
        resp.headers,
        body
      )
    }

  def make[I[_]: FlatMap, F[_]: Monad: Sync](implicit cache: Cache[F], logs: Logs[I, F]): I[HttpResponseCaching[F]] =
    logs.forService[HttpResponseCaching[F]].map { implicit l =>
      new RedisCaching[F](cache)
    }

  class RedisCaching[F[_]: Monad: Sync](cache: Cache[F]) extends HttpResponseCaching[F] {

    def process(req: Request[F]): F[Option[Response[F]]] =
      (for {
        requestBody <- OptionT.liftF(req.body.compile.to(Seq))
        bytes = req.method.toString.getBytes ++ req.uri.toString.getBytes ++ requestBody
        responseOpt <- OptionT(cache.get[Hash32, CachedResponse](Hash32(Blake2b256.hash(bytes))))
      } yield toResponse[F](responseOpt)).value

    def saveResponse(reqHash: Hash32, resp: Response[F]): F[Unit] =
      fromResponse(resp).flatMap { response =>
        cache.set[Hash32, CachedResponse](reqHash, response)
      }

    def invalidate: F[Unit] =
      cache.flushAll
  }
}
