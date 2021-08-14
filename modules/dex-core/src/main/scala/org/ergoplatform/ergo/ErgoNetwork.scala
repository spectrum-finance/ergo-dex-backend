package org.ergoplatform.ergo

import cats.syntax.either._
import cats.syntax.option._
import cats.tagless.FunctorK
import cats.tagless.syntax.functorK._
import cats.{FlatMap, Functor}
import derevo.derive
import fs2.Stream
import io.circe.Json
import io.circe.jawn.CirceSupportParser
import jawnfs2._
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.common.{ConstrainedEmbed, HexString}
import org.ergoplatform.dex.configs.NetworkConfig
import org.ergoplatform.dex.domain.errors.TxFailed
import org.ergoplatform.dex.protocol
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.dex.protocol.instances._
import org.ergoplatform.ergo.errors.ResponseError
import org.ergoplatform.ergo.explorer.models._
import org.ergoplatform.ergo.explorer.paths._
import org.ergoplatform.ergo.models.{EpochParams, NetworkInfo, Output, Transaction}
import org.typelevel.jawn.Facade
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3._
import sttp.client3.circe._
import sttp.model.MediaType
import tofu.MonadThrow
import tofu.fs2.LiftStream
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.raise._

@derive(representableK)
trait ErgoNetwork[F[_]] {

  /** Submit a transaction to the network.
    */
  def submitTransaction(tx: ErgoLikeTransaction): F[TxId]

  /** Check a given transaction.
    * @return None if transaction is valid, Some(error_description) otherwise.
    */
  def checkTransaction(tx: ErgoLikeTransaction): F[Option[String]]

  /** Get current network height.
    */
  def getCurrentHeight: F[Int]

  /** Get latest epoch params.
    */
  def getEpochParams: F[EpochParams]

  /** Get latest network info.
    */
  def getNetworkInfo: F[NetworkInfo]

  /** Get transactions which spend boxes with a given ErgoTree `templateHash`.
    */
  def getTransactionsByInputScript(templateHash: HexString, offset: Int, limit: Int): F[List[Transaction]]

  /** Get unspent outputs containing a given token.
    */
  def getUtxoByToken(tokenId: TokenId, offset: Int, limit: Int): F[List[Output]]
}

object ErgoNetwork {

  def make[
    I[_]: Functor,
    F[_]: MonadThrow: NetworkConfig.Has
  ](implicit backend: SttpBackend[F, Any], logs: Logs[I, F]): I[ErgoNetwork[F]] =
    logs.forService[ErgoNetwork[F]] map
    (implicit l => (context map (conf => new ErgoNetworkTracing[F] attach new CombinedErgoNetwork[F](conf))).embed)
}

final class ErgoNetworkTracing[F[_]: Logging: FlatMap] extends ErgoNetwork[Mid[F, *]] {

  def submitTransaction(tx: ErgoLikeTransaction): Mid[F, TxId] =
    for {
      _    <- trace"submitTransaction(tx=$tx)"
      txId <- _
      _    <- trace"submitTransaction(..) -> $txId"
    } yield txId

  def checkTransaction(tx: ErgoLikeTransaction): Mid[F, Option[String]] =
    for {
      _ <- trace"checkTransaction(tx=$tx)"
      r <- _
      _ <- trace"checkTransaction(..) -> $r"
    } yield r

  def getCurrentHeight: Mid[F, Int] =
    _.flatTap(i => trace"getCurrentHeight -> $i")

  def getEpochParams: Mid[F, EpochParams] =
    _.flatTap(p => trace"getEpochParams -> $p")

  def getNetworkInfo: Mid[F, NetworkInfo] =
    _.flatTap(i => trace"getNetworkInfo -> $i")

  def getTransactionsByInputScript(templateHash: HexString, offset: Int, limit: Int): Mid[F, List[Transaction]] =
    for {
      _   <- trace"getTransactionsByInputScript(templateHash=$templateHash, offset=$offset, limit=$limit)"
      txs <- _
      _   <- trace"getTransactionsByInputScript(..) -> $txs"
    } yield txs

  def getUtxoByToken(tokenId: TokenId, offset: Int, limit: Int): Mid[F, List[Output]] =
    for {
      _  <- trace"getUtxoByToken(tokenId=$tokenId, offset=$offset, limit=$limit)"
      os <- _
      _  <- trace"getUtxoByToken(..) -> $os"
    } yield os
}

class CombinedErgoNetwork[F[_]: MonadThrow](config: NetworkConfig)(implicit backend: SttpBackend[F, Any])
  extends ErgoNetwork[F] {

  private val explorerUri = config.explorerUri

  def submitTransaction(tx: ErgoLikeTransaction): F[TxId] =
    basicRequest
      .post(config.nodeUri withPathSegment node.paths.submitTransactionPathSeg)
      .contentType(MediaType.ApplicationJson)
      .body(tx)
      .response(asEither(asJsonAlways[node.models.ApiError], asString))
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

  def checkTransaction(tx: ErgoLikeTransaction): F[Option[String]] =
    basicRequest
      .post(explorerUri withPathSegment checkTransactionPathSeg)
      .contentType(MediaType.ApplicationJson)
      .body(tx)
      .response(asEither(asJsonAlways[ApiError], asJson[TxIdResponse]))
      .send(backend)
      .flatMap { res =>
        res.body match {
          case Left(err) =>
            err
              .leftMap(e => ResponseError(e.getMessage))
              .toRaise
              .map(e => Some(e.reason))
          case _ =>
            none[String].pure
        }
      }

  def getCurrentHeight: F[Int] =
    basicRequest
      .get(explorerUri.withPathSegment(blocksPathSeg).addParams("limit" -> "1", "order" -> "desc"))
      .response(asJson[Items[BlockInfo]])
      .send(backend)
      .flatMap(_.body.leftMap(resEx => ResponseError(resEx.getMessage)).toRaise)
      .map(_.items.headOption.map(_.height).getOrElse(protocol.constants.PreGenesisHeight))

  def getEpochParams: F[EpochParams] =
    basicRequest
      .get(explorerUri.withPathSegment(paramsPathSeg))
      .response(asJson[EpochParams])
      .send(backend)
      .flatMap(_.body.leftMap(resEx => ResponseError(resEx.getMessage)).toRaise)

  def getNetworkInfo: F[NetworkInfo] =
    basicRequest
      .get(explorerUri.withPathSegment(infoPathSeg))
      .response(asJson[NetworkInfo])
      .send(backend)
      .flatMap(_.body.leftMap(resEx => ResponseError(resEx.getMessage)).toRaise)

  def getTransactionsByInputScript(templateHash: HexString, offset: Int, limit: Int): F[List[Transaction]] =
    basicRequest
      .get(
        explorerUri
          .withPathSegment(txsByScriptsPathSeg(templateHash))
          .addParams("offset" -> offset.toString, "limit" -> limit.toString, "sortDirection" -> "asc")
      )
      .response(asJson[Items[Transaction]])
      .send(backend)
      .flatMap(_.body.leftMap(resEx => ResponseError(resEx.getMessage)).toRaise)
      .map(_.items)

  def getUtxoByToken(tokenId: TokenId, offset: Int, limit: Int): F[List[Output]] =
    basicRequest
      .get(
        explorerUri
          .withPathSegment(utxoByTokenIdPathSeg(tokenId))
          .addParams("offset" -> offset.toString, "limit" -> limit.toString)
      )
      .response(asJson[Items[Output]])
      .send(backend)
      .flatMap(_.body.leftMap(resEx => ResponseError(resEx.getMessage)).toRaise)
      .map(_.items)
}

trait ErgoNetworkStreaming[S[_], F[_]] extends ErgoNetwork[F] {

  /** Get a stream of unspent outputs at the given global offset.
    */
  def streamUnspentOutputs(boxGixOffset: Long, limit: Int): S[Output]
}

object ErgoNetworkStreaming {

  final private class StreamingContainer[S[_]: FlatMap, F[_]: FlatMap](tft: F[ErgoNetworkStreaming[S, F]])(implicit
    evals: Evals[S, F]
  ) extends ErgoNetworkStreaming[S, F] {

    def submitTransaction(tx: ErgoLikeTransaction): F[TxId] =
      tft.flatMap(_.submitTransaction(tx))

    def checkTransaction(tx: ErgoLikeTransaction): F[Option[String]] =
      tft.flatMap(_.checkTransaction(tx))

    def getCurrentHeight: F[Int] =
      tft.flatMap(_.getCurrentHeight)

    def getEpochParams: F[EpochParams] =
      tft.flatMap(_.getEpochParams)

    def getNetworkInfo: F[NetworkInfo] =
      tft.flatMap(_.getNetworkInfo)

    def getTransactionsByInputScript(templateHash: HexString, offset: Int, limit: Int): F[List[Transaction]] =
      tft.flatMap(_.getTransactionsByInputScript(templateHash, offset, limit))

    def getUtxoByToken(tokenId: TokenId, offset: Int, limit: Int): F[List[Output]] =
      tft.flatMap(_.getUtxoByToken(tokenId, offset, limit))

    def streamUnspentOutputs(boxGixOffset: Long, limit: Int): S[Output] =
      evals.eval(tft.map(_.streamUnspentOutputs(boxGixOffset, limit))).flatten
  }

  implicit def functorK[F[_]]: FunctorK[ErgoNetworkStreaming[*[_], F]] = {
    type Mod[S[_]] = ErgoNetworkStreaming[S, F]
    cats.tagless.Derive.functorK[Mod]
  }

  implicit def constEmbed[S[_]: FlatMap]: ConstrainedEmbed[ErgoNetworkStreaming[S, *[_]], Evals[S, *[_]]] =
    new ConstrainedEmbed[ErgoNetworkStreaming[S, *[_]], Evals[S, *[_]]] {

      def embed[F[_]: FlatMap: Evals[S, *[_]]](
        ft: F[ErgoNetworkStreaming[S, F]]
      ): ErgoNetworkStreaming[S, F] =
        new StreamingContainer[S, F](ft)
    }

  def make[
    S[_]: FlatMap: Evals[*[_], F]: LiftStream[*[_], F],
    F[_]: MonadThrow: NetworkConfig.Has
  ](implicit backend: SttpBackend[F, Fs2Streams[F]]): ErgoNetworkStreaming[S, F] =
    constEmbed[S].embed(
      context
        .map(conf => new ErgoExplorerStreaming[F](conf))
        .map(client => functorK.mapK(client)(LiftStream[S, F].liftF))
    )

  final class ErgoExplorerStreaming[
    F[_]: MonadThrow
  ](config: NetworkConfig)(implicit
    backend: SttpBackend[F, Fs2Streams[F]]
  ) extends CombinedErgoNetwork[F](config)
    with ErgoNetworkStreaming[Stream[F, *], F] {

    private val uri                           = config.explorerUri
    implicit private val facade: Facade[Json] = new CirceSupportParser(None, allowDuplicateKeys = false).facade

    def streamUnspentOutputs(boxGixOffset: Long, limit: Int): Stream[F, Output] = {
      val req =
        basicRequest
          .get(uri.withPathSegment(utxoPathSeg).addParams("minGix" -> boxGixOffset.toString, "limit" -> limit.toString))
          .response(asStreamAlwaysUnsafe(Fs2Streams[F]))
          .send(backend)
          .map(_.body)
      Stream
        .force(req)
        .chunks
        .parseJsonStream
        //.handleErrorWith(_ => Stream(Json.Null))
        .map(_.as[Output].toOption)
        .unNone
    }
  }
}
