package org.ergoplatform.ergo.services.explorer

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
import org.ergoplatform.common.sttp.syntax._
import org.ergoplatform.dex.configs.NetworkConfig
import org.ergoplatform.dex.protocol
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.dex.protocol.instances._
import org.ergoplatform.ergo.errors.ResponseError
import org.ergoplatform.ergo.services.explorer.models._
import org.ergoplatform.ergo.services.explorer.paths._
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.ergo.domain.{EpochParams, NetworkInfo}
import org.typelevel.jawn.Facade
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3._
import sttp.client3.circe._
import sttp.model.{MediaType, Uri}
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
trait ErgoExplorer[F[_]] {

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

  /** Get token info by token id.
    */
  def getTokenInfo(tokenId: TokenId): F[Option[TokenInfo]]
}

object ErgoExplorer {

  def make[
    I[_]: Functor,
    F[_]: MonadThrow: NetworkConfig.Has
  ](implicit backend: SttpBackend[F, Any], logs: Logs[I, F]): I[ErgoExplorer[F]] =
    logs.forService[ErgoExplorer[F]] map
    (implicit l => (context map (conf => new ErgoExplorerTracing[F] attach new CombinedErgoExplorer[F](conf))).embed)
}

final class ErgoExplorerTracing[F[_]: Logging: FlatMap] extends ErgoExplorer[Mid[F, *]] {

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

  def getTokenInfo(tokenId: TokenId): Mid[F, Option[TokenInfo]] =
    for {
      _  <- trace"getTokenInfo(tokenId=$tokenId)"
      os <- _
      _  <- trace"getTokenInfo(..) -> $os"
    } yield os
}

class CombinedErgoExplorer[F[_]: MonadThrow](config: NetworkConfig)(implicit backend: SttpBackend[F, Any])
  extends ErgoExplorer[F] {

  private val explorerUri = config.explorerUri

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
      .absorbError
      .map(_.items.headOption.map(_.height).getOrElse(protocol.constants.PreGenesisHeight))

  def getEpochParams: F[EpochParams] =
    basicRequest
      .get(explorerUri.withPathSegment(paramsPathSeg))
      .response(asJson[EpochParams])
      .send(backend)
      .absorbError

  def getNetworkInfo: F[NetworkInfo] =
    basicRequest
      .get(explorerUri.withPathSegment(infoPathSeg))
      .response(asJson[NetworkInfo])
      .send(backend)
      .absorbError

  def getTransactionsByInputScript(templateHash: HexString, offset: Int, limit: Int): F[List[Transaction]] =
    basicRequest
      .get(
        explorerUri
          .withPathSegment(txsByScriptsPathSeg(templateHash))
          .addParams("offset" -> offset.toString, "limit" -> limit.toString, "sortDirection" -> "asc")
      )
      .response(asJson[Items[Transaction]])
      .send(backend)
      .absorbError
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
      .absorbError
      .map(_.items)

  def getTokenInfo(tokenId: TokenId): F[Option[TokenInfo]] =
    basicRequest
      .get(explorerUri.withPathSegment(tokenInfoPathSeg(tokenId)))
      .response(asJson[TokenInfo])
      .send(backend)
      .map(_.body.toOption)
}

trait ErgoExplorerStreaming[S[_], F[_]] extends ErgoExplorer[F] {

  /** Get a stream of unspent outputs at the given global offset.
    */
  def streamUnspentOutputs(gOffset: Long, limit: Int): S[Output]

  /** Get a stream of unspent outputs at the given global offset.
    */
  def streamOutputs(gOffset: Long, limit: Int): S[Output]

  /** Get a stream of transactions at the given global offset.
    */
  def streamTransactions(gOffset: Long, limit: Int): S[Transaction]

  /** Get a stream of blocks at the given offset(height).
    */
  def streamBlocks(gOffset: Long, limit: Int): S[BlockInfo]
}

object ErgoExplorerStreaming {

  final private class StreamingContainer[S[_]: FlatMap, F[_]: FlatMap](tft: F[ErgoExplorerStreaming[S, F]])(implicit
    evals: Evals[S, F]
  ) extends ErgoExplorerStreaming[S, F] {

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

    def getTokenInfo(tokenId: TokenId): F[Option[TokenInfo]] =
      tft.flatMap(_.getTokenInfo(tokenId))

    def streamUnspentOutputs(gOffset: Long, limit: Int): S[Output] =
      evals.eval(tft.map(_.streamUnspentOutputs(gOffset, limit))).flatten

    def streamOutputs(gOffset: Long, limit: Int): S[Output] =
      evals.eval(tft.map(_.streamOutputs(gOffset, limit))).flatten

    def streamTransactions(gOffset: Long, limit: Int): S[Transaction] =
      evals.eval(tft.map(_.streamTransactions(gOffset, limit))).flatten

    def streamBlocks(gOffset: Long, limit: Int): S[BlockInfo] =
      evals.eval(tft.map(_.streamBlocks(gOffset, limit))).flatten
  }

  implicit def functorK[F[_]]: FunctorK[ErgoExplorerStreaming[*[_], F]] = {
    type Mod[S[_]] = ErgoExplorerStreaming[S, F]
    cats.tagless.Derive.functorK[Mod]
  }

  implicit def constEmbed[S[_]: FlatMap]: ConstrainedEmbed[ErgoExplorerStreaming[S, *[_]], Evals[S, *[_]]] =
    new ConstrainedEmbed[ErgoExplorerStreaming[S, *[_]], Evals[S, *[_]]] {

      def embed[F[_]: FlatMap: Evals[S, *[_]]](
        ft: F[ErgoExplorerStreaming[S, F]]
      ): ErgoExplorerStreaming[S, F] =
        new StreamingContainer[S, F](ft)
    }

  def make[
    S[_]: FlatMap: Evals[*[_], F]: LiftStream[*[_], F],
    F[_]: MonadThrow: NetworkConfig.Has
  ](implicit backend: SttpBackend[F, Fs2Streams[F]]): ErgoExplorerStreaming[S, F] =
    constEmbed[S].embed(
      context
        .map(conf => new ExplorerStreaming[F](conf))
        .map(client => functorK.mapK(client)(LiftStream[S, F].liftF))
    )

  final class ExplorerStreaming[
    F[_]: MonadThrow
  ](config: NetworkConfig)(implicit
    backend: SttpBackend[F, Fs2Streams[F]]
  ) extends CombinedErgoExplorer[F](config)
    with ErgoExplorerStreaming[Stream[F, *], F] {

    private val uri                           = config.explorerUri
    implicit private val facade: Facade[Json] = new CirceSupportParser(None, allowDuplicateKeys = false).facade

    def streamUnspentOutputs(gOffset: Long, limit: Int): Stream[F, Output] =
      streamSomeOutputs(utxoPathSeg)(gOffset, limit)

    def streamOutputs(gOffset: Long, limit: Int): Stream[F, Output] =
      streamSomeOutputs(txoPathSeg)(gOffset, limit)

    def streamTransactions(gOffset: Long, limit: Int): Stream[F, Transaction] = {
      val req =
        basicRequest
          .get(uri.withPathSegment(txPathSeg).addParams("minGix" -> gOffset.toString, "limit" -> limit.toString))
          .response(asStreamAlwaysUnsafe(Fs2Streams[F]))
          .send(backend)
          .map(_.body)
      Stream
        .force(req)
        .chunks
        .parseJsonStream
        .map(_.as[Transaction].toOption)
        .unNone
    }

    def streamBlocks(offset: Long, limit: Int): Stream[F, BlockInfo] = {
      val req =
        basicRequest
          .get(uri.withPathSegment(blocksStreamPathSeg).addParams("minGix" -> offset.toString, "limit" -> limit.toString))
          .response(asStreamAlwaysUnsafe(Fs2Streams[F]))
          .send(backend)
          .map(_.body)
      Stream
        .force(req)
        .chunks
        .parseJsonStream
        .map(_.as[BlockInfo].toOption)
        .unNone
    }

    def streamSomeOutputs(path: Uri.Segment)(boxGixOffset: Long, limit: Int): Stream[F, Output] = {
      val req =
        basicRequest
          .get(uri.withPathSegment(path).addParams("minGix" -> boxGixOffset.toString, "limit" -> limit.toString))
          .response(asStreamAlwaysUnsafe(Fs2Streams[F]))
          .send(backend)
          .map(_.body)
      Stream
        .force(req)
        .chunks
        .parseJsonStream
        .map(_.as[Output].toOption)
        .unNone
    }
  }
}
