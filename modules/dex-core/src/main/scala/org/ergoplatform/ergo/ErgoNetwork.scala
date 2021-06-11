package org.ergoplatform.ergo

import cats.FlatMap
import cats.syntax.either._
import cats.tagless.FunctorK
import cats.tagless.syntax.functorK._
import fs2.Stream
import io.circe.Json
import io.circe.jawn.CirceSupportParser
import jawnfs2._
import org.ergoplatform.ErgoLikeTransaction
import org.ergoplatform.common.{ConstrainedEmbed, HexString}
import org.ergoplatform.dex.configs.NetworkConfig
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.dex.protocol
import org.ergoplatform.ergo.errors.ResponseError
import org.ergoplatform.ergo.explorer.models._
import org.ergoplatform.ergo.explorer.paths._
import org.ergoplatform.ergo.models.{NetworkParams, Output, Transaction}
import org.typelevel.jawn.Facade
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3._
import sttp.client3.circe._
import sttp.model.{MediaType, Uri}
import tofu.fs2.LiftStream
import tofu.streams.Evals
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.{MonadThrow, WithContext}

trait ErgoNetwork[F[_]] {

  /** Submit a transaction to the network.
    */
  def submitTransaction(tx: ErgoLikeTransaction): F[TxId]

  /** Get current network height.
    */
  def getCurrentHeight: F[Int]

  /** Get actual network params.
    */
  def getNetworkParams: F[NetworkParams]

  /** Get transactions which spend boxes with a given ErgoTree `templateHash`.
    */
  def getTransactionsByInputScript(templateHash: HexString, offset: Int, limit: Int): F[List[Transaction]]

  /** Get unspent outputs containing a given token.
    */
  def getUtxoByToken(tokenId: TokenId, offset: Int, limit: Int): F[List[Output]]
}

trait StreamingErgoNetworkClient[S[_], F[_]] extends ErgoNetwork[F] {

  /** Get a stream of unspent outputs appeared in the network after `lastEpochs`.
    */
  def streamUnspentOutputs(lastEpochs: Int): S[Output]
}

object StreamingErgoNetworkClient {

  final private class ClientContainer[S[_]: FlatMap, F[_]: FlatMap](tft: F[StreamingErgoNetworkClient[S, F]])(implicit
    evals: Evals[S, F]
  ) extends StreamingErgoNetworkClient[S, F] {

    def submitTransaction(tx: ErgoLikeTransaction): F[TxId] =
      tft.flatMap(_.submitTransaction(tx))

    def getCurrentHeight: F[Int] =
      tft.flatMap(_.getCurrentHeight)

    def getNetworkParams: F[NetworkParams] =
      tft.flatMap(_.getNetworkParams)

    def getTransactionsByInputScript(templateHash: HexString, offset: Int, limit: Int): F[List[Transaction]] =
      tft.flatMap(_.getTransactionsByInputScript(templateHash, offset, limit))

    def getUtxoByToken(tokenId: TokenId, offset: Int, limit: Int): F[List[Output]] =
      tft.flatMap(_.getUtxoByToken(tokenId, offset, limit))

    def streamUnspentOutputs(lastEpochs: Int): S[Output] =
      evals.eval(tft.map(_.streamUnspentOutputs(lastEpochs))).flatten
  }

  implicit def functorK[F[_]]: FunctorK[StreamingErgoNetworkClient[*[_], F]] = {
    type Mod[S[_]] = StreamingErgoNetworkClient[S, F]
    cats.tagless.Derive.functorK[Mod]
  }

  implicit def constEmbed[S[_]: FlatMap]: ConstrainedEmbed[StreamingErgoNetworkClient[S, *[_]], Evals[S, *[_]]] =
    new ConstrainedEmbed[StreamingErgoNetworkClient[S, *[_]], Evals[S, *[_]]] {

      def embed[F[_]: FlatMap: Evals[S, *[_]]](
        ft: F[StreamingErgoNetworkClient[S, F]]
      ): StreamingErgoNetworkClient[S, F] =
        new ClientContainer[S, F](ft)
    }

  def make[
    S[_]: FlatMap: Evals[*[_], F]: LiftStream[*[_], F],
    F[_]: MonadThrow: WithContext[*[_], NetworkConfig]
  ](implicit backend: SttpBackend[F, Fs2Streams[F]]): StreamingErgoNetworkClient[S, F] =
    constEmbed[S].embed(
      context
        .map(conf => new ErgoExplorerClient[F](conf))
        .map(client => functorK.mapK(client)(LiftStream[S, F].liftF))
    )

  final class ErgoExplorerClient[
    F[_]: MonadThrow
  ](networkConfig: NetworkConfig)(implicit
    backend: SttpBackend[F, Fs2Streams[F]]
  ) extends StreamingErgoNetworkClient[Stream[F, *], F] {

    private val explorerHost                  = Uri.unsafeParse(networkConfig.explorerUri)
    implicit private val facade: Facade[Json] = new CirceSupportParser(None, allowDuplicateKeys = false).facade

    def submitTransaction(tx: ErgoLikeTransaction): F[TxId] =
      basicRequest
        .post(explorerHost withPathSegment submitTransactionPathSeg)
        .contentType(MediaType.ApplicationJson)
        .body(tx)
        .response(asJson[TxIdResponse])
        .send(backend)
        .flatMap(_.body.leftMap(resEx => ResponseError(resEx.getMessage)).toRaise)
        .map(_.id)

    def getCurrentHeight: F[Int] =
      basicRequest
        .get(explorerHost.withPathSegment(blocksPathSeg).addParams("limit" -> "1", "order" -> "desc"))
        .response(asJson[Items[BlockInfo]])
        .send(backend)
        .flatMap(_.body.leftMap(resEx => ResponseError(resEx.getMessage)).toRaise)
        .map(_.items.headOption.map(_.height).getOrElse(protocol.constants.PreGenesisHeight))

    def getNetworkParams: F[NetworkParams] =
      basicRequest
        .get(explorerHost.withPathSegment(paramsPathSeg))
        .response(asJson[NetworkParams])
        .send(backend)
        .flatMap(_.body.leftMap(resEx => ResponseError(resEx.getMessage)).toRaise)

    def getTransactionsByInputScript(templateHash: HexString, offset: Int, limit: Int): F[List[Transaction]] =
      basicRequest
        .get(
          explorerHost
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
          explorerHost
            .withPathSegment(utxoByTokenIdPathSeg(tokenId))
            .addParams("offset" -> offset.toString, "limit" -> limit.toString)
        )
        .response(asJson[Items[Output]])
        .send(backend)
        .flatMap(_.body.leftMap(resEx => ResponseError(resEx.getMessage)).toRaise)
        .map(_.items)

    def streamUnspentOutputs(lastEpochs: Int): Stream[F, Output] = {
      val req =
        basicRequest
          .get(explorerHost.withPathSegment(utxoPathSeg).addParams("lastEpochs" -> lastEpochs.toString))
          .response(asStreamAlwaysUnsafe(Fs2Streams[F]))
          .send(backend)
          .map(_.body)
      Stream
        .force(req)
        .chunks
        .parseJsonStream
        .handleErrorWith(_ => Stream(Json.Null))
        .map(_.as[Output].toOption)
        .unNone
    }
  }
}
