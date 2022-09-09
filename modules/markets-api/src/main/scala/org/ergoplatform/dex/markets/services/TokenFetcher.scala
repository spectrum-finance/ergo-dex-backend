package org.ergoplatform.dex.markets.services

import cats.effect.concurrent.Ref
import cats.effect.{Clock, Timer}
import cats.{FlatMap, Monad}
import derevo.derive
import io.circe.generic.auto._
import org.ergoplatform.common.sttp.syntax._
import org.ergoplatform.dex.markets.configs.TokenFetcherConfig
import org.ergoplatform.dex.protocol.codecs.{collBytesDecoder, fromTry}
import org.ergoplatform.ergo.TokenId
import sttp.client3.circe.asJson
import sttp.client3.{basicRequest, SttpBackend, UriContext}
import tofu.Throws
import tofu.concurrent.MakeRef
import tofu.higherKind.derived.representableK
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.time.now.millis
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

@derive(representableK)
trait TokenFetcher[F[_]] {
  def fetchTokens: F[List[TokenId]]
}

object TokenFetcher {

  val network = "ergo"

  val ergoToken: Token = Token(
    network,
    "0000000000000000000000000000000000000000000000000000000000000000",
    9,
    "Ergo",
    "ERG"
  )

  final case class Token(network: String, address: String, decimals: Int, name: String, ticker: String)
  final case class TokenResponse(tokens: List[Token])

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
        .get(uri"${conf.url}")
        .response(asJson[TokenResponse])
        .send(backend)
        .absorbError
        .map { resp =>
          (ergoToken :: resp.tokens).filter(_.network == network).map(tkn => TokenId.fromStringUnsafe(tkn.address))
        }
  }
}
