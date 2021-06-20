package org.ergoplatform.dex.executor.amm.repositories

import cats.Monad
import cats.syntax.either._
import derevo.derive
import org.ergoplatform.dex.domain.amm.state.Predicted
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.executor.amm.config.ResolverConfig
import org.ergoplatform.ergo.errors.ResponseError
import sttp.client3._
import sttp.client3.circe.asJson
import sttp.model.StatusCode
import tofu.higherKind.derived.embed
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.raise._

@derive(embed)
trait CFMMPools[F[_]] {

  /** Get pool state by pool id.
    */
  def get(id: PoolId): F[Option[CFMMPool]]

  /** Persist predicted pool.
    */
  def put(pool: Predicted[CFMMPool]): F[Unit]
}

object CFMMPools {

  def make[F[_]: Monad: ResponseError.Raise: ResolverConfig.Has](implicit
    backend: SttpBackend[F, Any]
  ): CFMMPools[F] =
    (context map (conf => new Live[F](conf): CFMMPools[F])).embed

  final class Live[F[_]: Monad: ResponseError.Raise](conf: ResolverConfig)(implicit
    backend: SttpBackend[F, Any]
  ) extends CFMMPools[F] {

    private val basePrefix = "/resolve/cfmm"

    def get(id: PoolId): F[Option[CFMMPool]] =
      basicRequest
        .get(conf.uri.withWholePath(s"$basePrefix/$id"))
        .response(asJson[CFMMPool])
        .send(backend)
        .flatMap { r =>
          if (r.code == StatusCode.NotFound) Option.empty[CFMMPool].pure
          else r.body.leftMap(e => ResponseError(e.getMessage)).map(Option(_)).toRaise[F]
        }

    def put(pool: Predicted[CFMMPool]): F[Unit] =
      basicRequest
        .post(conf.uri.withWholePath(basePrefix))
        .send(backend)
        .void
  }
}
