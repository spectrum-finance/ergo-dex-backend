package org.ergoplatform.dex.executor.amm.repositories

import cats.syntax.either._
import cats.{FlatMap, Functor, Monad}
import derevo.derive
import org.ergoplatform.common.TraceId
import org.ergoplatform.common.http.Tracing
import org.ergoplatform.dex.domain.amm.state.Predicted
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.executor.amm.config.ResolverConfig
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.errors.ResponseError
import sttp.client3._
import sttp.client3.circe._
import sttp.model.StatusCode
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.raise._

@derive(representableK)
trait CFMMPools[F[_]] {

  /** Get pool state by pool id.
    */
  def get(id: PoolId): F[Option[CFMMPool]]

  /** Persist predicted pool.
    */
  def put(pool: Predicted[CFMMPool]): F[Unit]

  /** Invalidate pool prediction.
    */
  def invalidate(boxId: BoxId): F[Unit]
}

object CFMMPools {

  def make[I[_]: Functor, F[_]: Monad: ResponseError.Raise: ResolverConfig.Has: TraceId.Has](implicit
    backend: SttpBackend[F, Any],
    logs: Logs[I, F]
  ): I[CFMMPools[F]] =
    logs.forService[CFMMPools[F]] map { implicit l =>
      (ResolverConfig.access map (conf => new CFMMPoolsTracing[F] attach new Live[F](conf))).embed
    }

  final class Live[F[_]: Monad: ResponseError.Raise: TraceId.Has](conf: ResolverConfig)(implicit
    backend: SttpBackend[F, Any]
  ) extends CFMMPools[F] {

    private val BasePrefix = "/cfmm"

    private val mkBasicReq = context map { traceId => basicRequest.header(Tracing.TraceIdHeader, traceId.value) }

    def get(id: PoolId): F[Option[CFMMPool]] =
      mkBasicReq.flatMap { base =>
        base
          .get(conf.uri.withWholePath(s"$BasePrefix/resolve/$id"))
          .response(asJson[CFMMPool])
          .send(backend)
          .flatMap { r =>
            if (r.code == StatusCode.NotFound) Option.empty[CFMMPool].pure
            else r.body.leftMap(e => ResponseError(e.getMessage)).map(Option(_)).toRaise[F]
          }
      }

    def put(pool: Predicted[CFMMPool]): F[Unit] =
      mkBasicReq.flatMap { base =>
        base
          .post(conf.uri.withWholePath(s"$BasePrefix/predicted"))
          .body(pool)
          .send(backend)
          .flatMap { r =>
            if (r.isSuccess) unit
            else ResponseError(s"Non 2xx response code. ${r.body}").raise
          }
      }

    def invalidate(boxId: BoxId): F[Unit] =
      mkBasicReq.flatMap { base =>
        base
          .post(conf.uri.withWholePath(s"$BasePrefix/invalidate/$boxId"))
          .send(backend)
          .flatMap { r =>
            if (r.isSuccess) unit
            else ResponseError(s"Non 2xx response code. ${r.body}").raise
          }
      }
  }

  final class CFMMPoolsTracing[F[_]: FlatMap: Logging] extends CFMMPools[Mid[F, *]] {

    def get(id: PoolId): Mid[F, Option[CFMMPool]] =
      for {
        _ <- trace"get(id=$id)"
        r <- _
        _ <- trace"get(id=$id) -> $r"
      } yield r

    def put(pool: Predicted[CFMMPool]): Mid[F, Unit] =
      for {
        _ <- trace"put(pool=$pool)"
        r <- _
        _ <- trace"put(pool=$pool) -> $r"
      } yield r

    def invalidate(boxId: BoxId): Mid[F, Unit] =
      for {
        _ <- trace"invalidate(boxId=$boxId)"
        r <- _
        _ <- trace"invalidate(boxId=$boxId) -> $r"
      } yield r
  }
}
