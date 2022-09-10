package org.ergoplatform.dex.executor.amm.generators

import cats.effect.{Clock, Sync}
import org.ergoplatform.dex.domain.amm.{PoolId, Swap, SwapParams}
import cats.syntax.traverse._
import org.ergoplatform.common.HexString
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.executor.amm.generators.ErgoTreeGenerator.serializedSwapTree
import org.ergoplatform.ergo.{BoxId, PubKey, TokenId, TxId}
import org.ergoplatform.ergo.domain.{BoxAsset, Output}
import tofu.generate.GenRandom
import tofu.syntax.time.now
import tofu.syntax.monadic._

import scala.util.Random

object OrdersGenerator {

  def genSwapOrders[F[+_]: Sync: Clock: GenRandom](qty: Int): F[List[Swap]] =
    (0 until qty).toList.traverse(_ => genSwapOrder[F])

  def genSwapOrder[F[_]: Sync: GenRandom: Clock]: F[Swap] = for {
    timestamp   <- now.millis
    maxMinerFee <- GenRandom.nextLong
    tokenHex    <- randomHexString
    inputAsset  <- genAssetAmount
    params      <- genDummySwapParams(inputAsset)
    box         <- genDummyOutput(inputAsset)
  } yield Swap(
    PoolId(TokenId(tokenHex)),
    maxMinerFee,
    timestamp,
    params,
    box
  )

  def genDummySwapParams[F[_]: GenRandom: Sync](input: AssetAmount): F[SwapParams] = for {
    minOutput           <- genAssetAmount
    dexFeePerTokenNum   <- GenRandom.nextLong
    dexFeePerTokenDenom <- GenRandom.nextLong
    pubKeyHex           <- randomHexString
  } yield SwapParams(
    input,
    minOutput,
    dexFeePerTokenNum,
    dexFeePerTokenDenom,
    PubKey(pubKeyHex)
  )

  def genDummyOutput[F[_]: Sync: GenRandom](input: AssetAmount): F[Output] = for {
    boxIdRaw       <- randomString(32)
    txIdRaw        <- randomString(32)
    value          <- GenRandom.nextLong
    index          <- GenRandom.nextInt(32)
    creationHeight <- GenRandom.nextInt(32)
  } yield Output(
    BoxId(boxIdRaw),
    TxId(txIdRaw),
    value,
    index,
    creationHeight,
    serializedSwapTree,
    List(BoxAsset(input.id, input.value)),
    Map.empty
  )

  def genAssetAmount[F[_]: GenRandom: Sync]: F[AssetAmount] = for {
    assetId <- randomHexString
    value   <- GenRandom.nextLong
  } yield AssetAmount(TokenId(assetId), value)

  def randomHexString[F[_]: Sync] =
    randomString(32).map(str => HexString.fromBytes(str.getBytes))

  def randomString[F[_]: Sync](length: Int): F[String] =
    Sync[F].delay(Random.alphanumeric.take(length).mkString)
}
