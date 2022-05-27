package org.ergoplatform.dex.markets.services

import cats.Monad
import cats.effect.{Blocker, ContextShift, Sync}
import cats.syntax.either._
import derevo.derive
import fs2.io.file._
import io.circe.Decoder
import io.circe.parser._
import org.ergoplatform.dex.markets.configs.TokenFetcherConfig
import org.ergoplatform.ergo.TokenId
import sttp.client3.SttpBackend
import tofu.Throws
import tofu.higherKind.derived.representableK
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.raise._
import java.nio.file.Paths

@derive(representableK)
trait TokenFetcher[F[_]] {
  def fetchTokens: F[List[TokenId]]
}

object TokenFetcher {

  val chunkSize = 1024

  def make[F[_]: Monad: Throws: Sync: ContextShift: TokenFetcherConfig.Has](blocker: Blocker)(implicit
    backend: SttpBackend[F, Any]
  ): TokenFetcher[F] =
    context.map(conf => new ValidTokensFetcher[F](conf, blocker): TokenFetcher[F]).embed

  final class ValidTokensFetcher[F[_]: Monad: Throws: Sync: ContextShift](conf: TokenFetcherConfig, blocker: Blocker)(
    implicit backend: SttpBackend[F, Any]
  ) extends TokenFetcher[F] {

    def fetchTokens: F[List[TokenId]] =
      readAll(Paths.get(conf.filePath), blocker, chunkSize).compile.toList.flatMap { body =>
        parseTokens[List[TokenId]](new String(body.toArray)).toRaise
      }

    def parseTokens[A: Decoder](s: String): Either[Throwable, A] =
      for {
        json <- parse(s).leftMap(_.underlying)
        res  <- decode[A](json.toString()).leftMap(err => new Throwable(err.getMessage))
      } yield res
  }
}
