package org.ergoplatform.common.http.cache

import cats.data.OptionT
import cats.effect.Sync
import cats.{FlatMap, Monad}
import io.circe.syntax.EncoderOps
import io.circe.{jawn, Json}
import org.ergoplatform.common.cache.Cache
import org.ergoplatform.common.http.cache.models.ResponseDB
import org.ergoplatform.common.http.cache.models.ResponseDB._
import org.http4s._
import scodec.codecs.implicits._
import tofu.logging.Logs
import tofu.syntax.embed._
import tofu.syntax.monadic.{TofuFlatMapOps, TofuFunctorOps}

trait HttpResponseCaching[F[_]] {

  def process(req: Request[F]): F[Option[Response[F]]]

  def saveResponse(reqHash: Int, resp: Response[F]): F[Unit]

  def invalidate: F[Unit]
}

object HttpResponseCaching {

  private def respAsJson[F[_]: Sync](resp: Response[F]): F[Json] =
    resp.body.compile.to(Array).map { body =>
      ResponseDB(
        resp.status,
        resp.httpVersion,
        resp.headers,
        body
      ).asJson
    }

  def make[I[_]: FlatMap, F[_]: Monad: Sync](implicit cache: Cache[F], logs: Logs[I, F]): I[HttpResponseCaching[F]] =
    logs.forService[HttpResponseCaching[F]].map { implicit l =>
      new RedisCaching[F](cache)
    }

  class RedisCaching[F[_]: Monad: Sync](cache: Cache[F]) extends HttpResponseCaching[F] {

    def process(req: Request[F]): F[Option[Response[F]]] =
      (for {
        responseOpt <- OptionT(cache.get[Int, String](req.hashCode))
        response    <- OptionT.fromOption(jawn.decode[ResponseDB](responseOpt).toOption.map(respFromDB[F]))
      } yield response).value

    def saveResponse(reqHash: Int, resp: Response[F]): F[Unit] =
      respAsJson(resp).flatMap { response =>
        cache.set[Int, String](reqHash, response.toString)
      }

    def invalidate: F[Unit] =
      cache.flushAll
  }
}
