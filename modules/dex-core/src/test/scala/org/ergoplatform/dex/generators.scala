package org.ergoplatform.dex

import eu.timepit.refined.refineV
import eu.timepit.refined.string.HexStringSpec
import org.bouncycastle.util.BigIntegers
import org.ergoplatform.P2PKAddress
import org.ergoplatform.common.HexString
import org.ergoplatform.contracts.DexLimitOrderContracts._
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexSellerContractParameters}
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.domain.orderbook.{Order, OrderMeta}
import org.ergoplatform.dex.domain.{AssetAmount, BoxInfo}
import org.ergoplatform.dex.implicits._
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{Address, BoxId, TokenId}
import org.scalacheck.Gen
import scorex.crypto.hash.Blake2b256
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol.{DLogProverInput, ProveDlog}

object generators {

  def rawHexStringGen: Gen[String] =
    Gen
      .nonEmptyListOf(Gen.alphaNumChar)
      .map(chars => Base16.encode(Blake2b256.hash(chars.mkString)))

  def hexStringGen: Gen[HexString] =
    rawHexStringGen
      .map(x => refineV[HexStringSpec](x).right.get)
      .map(HexString.apply)

  def tokenIdGen: Gen[TokenId] =
    hexStringGen map TokenId.apply

  def boxIdGen: Gen[BoxId] =
    rawHexStringGen map BoxId.apply

  def p2PkAddressGen: Gen[P2PKAddress] =
    for {
      key <- Gen.listOfN(32, Gen.posNum[Byte]).map(_.toArray)
      dlog = DLogProverInput(BigIntegers.fromUnsignedByteArray(key)).publicImage
    } yield new P2PKAddress(dlog, dlog.pkBytes)

  def addressGen: Gen[Address] =
    p2PkAddressGen.map { p2pk =>
      Address.fromStringUnsafe(p2pk.encoder.toString(p2pk))
    }

  def proveDlogGen: Gen[ProveDlog] =
    Gen.delay(DLogProverInput.random()).map(_.publicImage)

  def orderMetaBuyerGen(boxValue: Long, quoteAssetId: TokenId, price: Long, feePerToken: Long): Gen[OrderMeta] =
    for {
      boxId <- boxIdGen
      ts    <- Gen.choose(1601447470169L, 1603447470169L)
      pk    <- proveDlogGen
      script = buyerContractInstance(DexBuyerContractParameters(pk, quoteAssetId.toErgo, price, feePerToken))
    } yield OrderMeta(boxId, boxValue, script.ergoTree, pk, ts)

  def orderMetaSellerGen(boxValue: Long, quoteAssetId: TokenId, price: Long, feePerToken: Long): Gen[OrderMeta] =
    for {
      boxId <- boxIdGen
      ts    <- Gen.choose(1601447470169L, 1603447470169L)
      pk    <- proveDlogGen
      script = sellerContractInstance(DexSellerContractParameters(pk, quoteAssetId.toErgo, price, feePerToken))
    } yield OrderMeta(boxId, boxValue, script.ergoTree, pk, ts)

  def bidGen(
    quoteAsset: TokenId,
    baseAsset: TokenId,
    amount: Long,
    price: Long,
    feePerToken: Long
  ): Gen[Order.Bid] =
    orderMetaBuyerGen(amount * (price + feePerToken), quoteAsset, price, feePerToken) flatMap
    (meta => Order.mkBid(quoteAsset, baseAsset, amount, price, feePerToken, meta))

  def askGen(
    quoteAsset: TokenId,
    baseAsset: TokenId,
    amount: Long,
    price: Long,
    feePerToken: Long
  ): Gen[Order.Ask] =
    orderMetaSellerGen(amount * feePerToken, quoteAsset, price, feePerToken) flatMap
    (meta => Order.mkAsk(quoteAsset, baseAsset, amount, price, feePerToken, meta))

  def priceGen: Gen[Long] =
    Gen.chooseNum(2L, 1000L)

  def feeGen: Gen[Long] =
    Gen.chooseNum(1000L, 1000000000L)

  def assetAmountGen(value: Long): Gen[AssetAmount] =
    for {
      id     <- tokenIdGen
      ticker <- Gen.alphaNumStr.map(_.take(3).map(_.toUpper))
    } yield AssetAmount(id, value, Some(ticker))

  def assetAmountGen: Gen[AssetAmount] =
    Gen.posNum[Long].flatMap(assetAmountGen)

  def cfmmPoolGen(gix: Long, reservesX: Long, reservesY: Long): Gen[CFMMPool] =
    for {
      poolId <- hexStringGen.map(PoolId.fromHex)
      lp     <- assetAmountGen
      x      <- assetAmountGen(reservesX)
      y      <- assetAmountGen(reservesY)
      feeNum <- Gen.const(995)
      boxId  <- boxIdGen
      box = BoxInfo(boxId, 1000000, gix)
    } yield CFMMPool(poolId, lp, x, y, feeNum, box)

  def cfmmPoolGen(gix: Long): Gen[CFMMPool] =
    for {
      x    <- Gen.posNum[Long]
      y    <- Gen.posNum[Long]
      pool <- cfmmPoolGen(gix, x, y)
    } yield pool

  def cfmmPoolGen: Gen[CFMMPool] =
    for {
      gix  <- Gen.posNum[Int].map(_.toLong)
      x    <- Gen.posNum[Long]
      y    <- Gen.posNum[Long]
      pool <- cfmmPoolGen(gix, x, y)
    } yield pool

  def cfmmPoolPredictionsGen(len: Int): Gen[List[CFMMPool]] =
    for {
      root  <- cfmmPoolGen
      pools <- Gen.listOfN(len, cfmmPoolGen(root.box.lastConfirmedBoxGix)).map(_.map(_.copy(poolId = root.poolId)))
    } yield root :: pools
}
