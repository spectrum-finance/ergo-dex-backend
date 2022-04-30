package org.ergoplatform.dex.markets.services

import cats.Monad
import cats.syntax.either._
import derevo.derive
import io.circe.Decoder
import io.circe.parser._
import org.ergoplatform.common.sttp.syntax._
import org.ergoplatform.dex.markets.configs.TokenFetcherConfig
import org.ergoplatform.ergo.TokenId
import sttp.client3.{SttpBackend, UriContext, asString, basicRequest}
import tofu.Throws
import tofu.higherKind.derived.representableK
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.raise._
import scala.util.Try

@derive(representableK)
trait TokenFetcher[F[_]] {
  def fetchTokens: F[List[TokenId]]
}

object TokenFetcher {

  def make[F[_]: Monad: Throws: TokenFetcherConfig.Has](implicit backend: SttpBackend[F, Any]): TokenFetcher[F] =
    context.map(conf => new ValidTokensFetcher[F](conf): TokenFetcher[F]).embed

  class ValidTokensFetcher[F[_]: Monad: Throws: TokenFetcherConfig.Has](conf: TokenFetcherConfig)(implicit
    backend: SttpBackend[F, Any]
  ) extends TokenFetcher[F] {

    def fetchTokens: F[List[TokenId]] =
      basicRequest
        .get(uri"${conf.filePath}")
        .response(asString)
        .send(backend)
        .absorbError
        .flatMap { body =>
          parseTokens[List[TokenId]](body).toRaise
        }

    def parseTokens[A: Decoder](s: String): Either[Throwable, A] =
      for {
        json <- parse(s).leftMap(_.underlying)
        content <- (json \\ "content")
                     .map(k => StringContext.processEscapes(k.toString()).filter(_ >= ' '))
                     .headOption
                     .toRight(new Throwable("Empty file"))
        decoded <- Try(java.util.Base64.getMimeDecoder.decode(content)).toEither
        res     <- decode[A](new String(decoded)).leftMap(err => new Throwable(err.getMessage))
      } yield res
  }
}
