package org.ergoplatform.dex.markets.services

import cats.effect.{Clock, Timer}
import cats.effect.concurrent.Ref
import cats.syntax.either._
import cats.{FlatMap, Monad}
import derevo.derive
import io.circe.Decoder
import io.circe.parser._
import org.ergoplatform.common.sttp.syntax._
import org.ergoplatform.dex.markets.configs.TokenFetcherConfig
import org.ergoplatform.ergo.TokenId
import sttp.client3.{asString, basicRequest, SttpBackend, UriContext}
import tofu.Throws
import tofu.concurrent.MakeRef
import tofu.higherKind.derived.representableK
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.syntax.time.now.millis
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

@derive(representableK)
trait TokenFetcher[F[_]] {
  def fetchTokens: F[List[TokenId]]
}

object TokenFetcher {

  def make[I[_]: FlatMap, F[_]: Clock: Timer: Monad: Throws: TokenFetcherConfig.Has](implicit
    backend: SttpBackend[F, Any],
    makeRef: MakeRef[I, F]
  ): I[TokenFetcher[F]] =
    makeRef.refOf((0L, List.empty[TokenId])).map { tokenRef =>
      TokenFetcherConfig.access.map(conf => new ValidTokensFetcher[F](tokenRef, conf): TokenFetcher[F]).embed
    }

  final class ValidTokensFetcher[F[_]: Clock: Timer: Monad: Throws](
    tokenRef: Ref[F, (Long, List[TokenId])],
    conf: TokenFetcherConfig
  )(implicit
    backend: SttpBackend[F, Any]
  ) extends TokenFetcher[F] {

    def fetchTokens: F[List[TokenId]] =
      for {
        (ts, cachedTokens) <- tokenRef.get
        currTs             <- millis
        tokens <- if (FiniteDuration(currTs - ts, TimeUnit.MILLISECONDS) > conf.rate)
                    requestedTokens.flatMap(tokens => tokenRef.set((currTs, tokens)).map(_ => tokens))
                  else cachedTokens.pure
      } yield tokens

    private val requestedTokens: F[List[TokenId]] =
      basicRequest
        .get(uri"${conf.filePath}")
        .response(asString)
        .send(backend)
        .absorbError
        .flatMap(res => parseTokens[List[TokenId]](res).toRaise)

    private def parseTokens[A: Decoder](s: String): Either[Throwable, A] =
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
