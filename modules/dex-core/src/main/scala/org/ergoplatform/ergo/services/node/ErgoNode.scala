package org.ergoplatform.ergo.services.node

import cats.{FlatMap, Functor}
import derevo.derive
import cats.syntax.either._
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.dex.configs.NetworkConfig
import org.ergoplatform.dex.domain.errors.TxFailed
import org.ergoplatform.ergo.TxId
import org.ergoplatform.ergo.errors.ResponseError
import org.ergoplatform.common.sttp.syntax._
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.dex.protocol.instances._
import org.ergoplatform.ergo.services.node.models.Transaction
import sttp.client3.{asEither, asString, basicRequest, SttpBackend}
import sttp.client3._
import sttp.client3.circe._
import sttp.model.MediaType
import tofu.MonadThrow
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.syntax.embed._
import tofu.syntax.logging._

@derive(representableK)
trait ErgoNode[F[_]] {

  /** Submit a transaction to the network.
    */
  def submitTransaction(tx: ErgoLikeTransaction): F[TxId]

  /** Get transactions from mempool.
    */
  def unconfirmedTransactions(offset: Int, limit: Int): F[Vector[Transaction]]
}

object ErgoNode {

  def make[
    I[_]: Functor,
    F[_]: MonadThrow: NetworkConfig.Has
  ](implicit backend: SttpBackend[F, Any], logs: Logs[I, F]): I[ErgoNode[F]] =
    logs.forService[ErgoNode[F]] map
    (implicit l => (NetworkConfig.access map (conf => new ErgoNodeTracing[F] attach new Live[F](conf))).embed)

  final class Live[F[_]: MonadThrow](config: NetworkConfig)(implicit backend: SttpBackend[F, Any]) extends ErgoNode[F] {

    def submitTransaction(tx: ErgoLikeTransaction): F[TxId] =
      basicRequest
        .post(config.nodeUri withPathSegment paths.submitTransactionPathSeg)
        .contentType(MediaType.ApplicationJson)
        .body(tx)
        .response(asEither(asJsonAlways[org.ergoplatform.ergo.services.node.models.ApiError], asString))
        .send(backend)
        .flatMap { res =>
          res.body match {
            case Left(err) =>
              err
                .leftMap(e => ResponseError(e.getMessage))
                .toRaise
                .flatMap(e => TxFailed(e.detail).raise)
            case Right(r) =>
              r.leftMap(ResponseError(_))
                .toRaise
                .map(s => TxId(s.replace("\"", "")))
          }
        }

    def unconfirmedTransactions(offset: Int, limit: Int): F[Vector[Transaction]] =
      basicRequest.get(config.nodeUri withPathSegment paths.unconfirmedTransactionsPathSeg)
        .response(asJson[Vector[Transaction]])
        .send(backend)
        .absorbError
  }

  final class ErgoNodeTracing[F[_]: Logging: FlatMap] extends ErgoNode[Mid[F, *]] {

    def submitTransaction(tx: ErgoLikeTransaction): Mid[F, TxId] =
      for {
        _    <- trace"submitTransaction(tx=$tx)"
        txId <- _
        _    <- trace"submitTransaction(..) -> $txId"
      } yield txId

    def unconfirmedTransactions(offset: Int, limit: Int): Mid[F, Vector[Transaction]] =
      for {
        _   <- trace"unconfirmedTransactions(offset=$offset, limit=$limit)"
        txs <- _
        _   <- trace"unconfirmedTransactions(offset=$offset, limit=$limit) -> List(..){n=${txs.size}}"
      } yield txs
  }
}
