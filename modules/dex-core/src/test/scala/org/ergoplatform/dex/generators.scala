package org.ergoplatform.dex

import eu.timepit.refined.refineV
import eu.timepit.refined.string.HexStringSpec
import org.bouncycastle.util.BigIntegers
import org.ergoplatform.P2PKAddress
import org.ergoplatform.contracts.{DexBuyerContractParameters, DexSellerContractParameters}
import org.ergoplatform.dex.domain.orderbook.{Order, OrderMeta}
import org.ergoplatform.dex.domain.syntax.ergo._
import org.scalacheck.Gen
import scorex.crypto.hash.Blake2b256
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol.{DLogProverInput, ProveDlog}
import org.ergoplatform.dex.implicits._
import org.ergoplatform.contracts.DexLimitOrderContracts._

object generators {

  def rawHexStringGen: Gen[String] =
    Gen
      .nonEmptyListOf(Gen.alphaNumChar)
      .map(chars => Base16.encode(Blake2b256.hash(chars.mkString)))

  def hexStringGen: Gen[HexString] =
    rawHexStringGen
      .map(x => refineV[HexStringSpec](x).right.get)
      .map(HexString.apply)

  def assetIdGen: Gen[AssetId] =
    hexStringGen map AssetId.apply

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

  def orderMetaBuyerGen(boxValue: Long, quoteAssetId: AssetId, price: Long, feePerToken: Long): Gen[OrderMeta] =
    for {
      boxId <- boxIdGen
      ts    <- Gen.choose(1601447470169L, 1603447470169L)
      pk    <- proveDlogGen
      script = buyerContractInstance(DexBuyerContractParameters(pk, quoteAssetId.toErgo, price, feePerToken))
    } yield OrderMeta(boxId, boxValue, script.ergoTree, pk, ts)

  def orderMetaSellerGen(boxValue: Long, quoteAssetId: AssetId, price: Long, feePerToken: Long): Gen[OrderMeta] =
    for {
      boxId <- boxIdGen
      ts    <- Gen.choose(1601447470169L, 1603447470169L)
      pk    <- proveDlogGen
      script = sellerContractInstance(DexSellerContractParameters(pk, quoteAssetId.toErgo, price, feePerToken))
    } yield OrderMeta(boxId, boxValue, script.ergoTree, pk, ts)

  def bidGen(
    quoteAsset: AssetId,
    baseAsset: AssetId,
    amount: Long,
    price: Long,
    feePerToken: Long
  ): Gen[Order.Bid] =
    orderMetaBuyerGen(amount * (price + feePerToken), quoteAsset, price, feePerToken) flatMap
    (meta => Order.mkBid(quoteAsset, baseAsset, amount, price, feePerToken, meta))

  def askGen(
    quoteAsset: AssetId,
    baseAsset: AssetId,
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
}
