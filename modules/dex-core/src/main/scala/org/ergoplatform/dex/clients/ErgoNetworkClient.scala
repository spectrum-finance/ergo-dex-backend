package org.ergoplatform.dex.clients

import cats.Monad
import cats.syntax.either._
import derevo.derive
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.TxId
import org.ergoplatform.dex.clients.errors.ResponseError
import org.ergoplatform.dex.explorer.models.{BlockInfo, Items, TxIdResponse}
import org.ergoplatform.dex.configs.NetworkConfig
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.dex.explorer.constants._
import sttp.client3._
import sttp.client3.circe._
import sttp.model.{MediaType, Uri}
import tofu.higherKind.derived.representableK
import tofu.{Raise, WithContext}
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.syntax.context._
import tofu.syntax.embed._

@derive(representableK)
trait ErgoNetworkClient[F[_]] {

  /** Submit a transaction to the network.
    */
  def submitTransaction(tx: ErgoLikeTransaction): F[TxId]

  /** Get current network height.
    */
  def getCurrentHeight: F[Int]
}

object ErgoNetworkClient {

  def make[F[_]: Monad: Raise[*[_], ResponseError]: WithContext[*[_], NetworkConfig]](implicit
    backend: SttpBackend[F, Any]
  ): ErgoNetworkClient[F] =
    context.map(conf => new ErgoExplorerNetworkClient[F](conf): ErgoNetworkClient[F]).embed

  final private class ErgoExplorerNetworkClient[
    F[_]: Monad: Raise[*[_], ResponseError]
  ](networkConfig: NetworkConfig)(implicit
    backend: SttpBackend[F, Any]
  ) extends ErgoNetworkClient[F] {

    private val explorerUri = Uri.unsafeApply(networkConfig.explorerUri)

    def submitTransaction(tx: ErgoLikeTransaction): F[TxId] =
      basicRequest
        .post(explorerUri withPathSegment submitTransactionPathSeg)
        .contentType(MediaType.ApplicationJson)
        .body(tx)
        .response(asJson[TxIdResponse])
        .send(backend)
        .flatMap(_.body.leftMap(resEx => ResponseError(resEx.getMessage)).toRaise)
        .map(_.id)

    def getCurrentHeight: F[Int] =
      basicRequest
        .get(explorerUri.withPathSegment(blocksPathSeg).addParams("limit" -> "1", "order" -> "desc"))
        .response(asJson[Items[BlockInfo]])
        .send(backend)
        .flatMap(_.body.leftMap(resEx => ResponseError(resEx.getMessage)).toRaise)
        .map(_.items.headOption.map(_.height).getOrElse(0))
  }
}
