package org.ergoplatform.dex.markets.services

import cats.Monad
import cats.syntax.either._
import derevo.derive
import io.circe.Decoder
import io.circe.parser._
import org.ergoplatform.common.sttp.syntax._
import org.ergoplatform.dex.markets.configs.TokenFetcherConfig
import org.ergoplatform.ergo.TokenId
import sttp.client3.{asString, basicRequest, SttpBackend, UriContext}
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

  final class ValidTokensFetcher[F[_]: Monad: Throws](conf: TokenFetcherConfig)(implicit
    backend: SttpBackend[F, Any]
  ) extends TokenFetcher[F] {

    def fetchTokens: F[List[TokenId]] =
      List(
        "0000000000000000000000000000000000000000000000000000000000000000",
        "003bd19d0187117f130b62e1bcab0939929ff5c7709f843c5c4dd158949285d0",
        "03faf2cb329f2e90d6d23b58d91bbb6c046aa143261cc21f52fbe2824bfcbf04",
        "0cd8c9f416e5b1ca9f986a7f10a84191dfb85941619e49e53c0dc30ebf83324b",
        "36aba4b4a97b65be491cf9f5ca57b5408b0da8d0194f30ec8330d1e8946161c1",
        "472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e8",
        "5a34d53ca483924b9a6aa0c771f11888881b516a8d1a9cdc535d063fe26d065e",
        "d71693c49a84fbbecd4908c94813b46514b18b67a99952dc1e6e4791556de413",
        "1fd6e032e8476c4aa54c18c1a308dce83940e8f4a28f576440513ed7326ad489",
        "00b1e236b60b95c2c6f8007a9d89bc460fc9e78f98b09faec9449007b40bccf3",
        "007fd64d1ee54d78dd269c8930a38286caa28d3f29d27cadcb796418ab15c283",
        "02f31739e2e4937bb9afb552943753d1e3e9cdd1a5e5661949cb0cef93f907ea"
      ).map(TokenId.fromStringUnsafe).pure
  }
}
