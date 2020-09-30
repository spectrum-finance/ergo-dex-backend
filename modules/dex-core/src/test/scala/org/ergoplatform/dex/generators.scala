package org.ergoplatform.dex

import eu.timepit.refined.refineV
import eu.timepit.refined.string.HexStringSpec
import org.bouncycastle.util.BigIntegers
import org.ergoplatform.P2PKAddress
import org.ergoplatform.dex.domain.models.{Order, OrderMeta}
import org.scalacheck.Gen
import scorex.crypto.hash.Blake2b256
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol.DLogProverInput
import org.ergoplatform.dex.implicits._

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

  def orderMetaGen(boxValue: Long, address: P2PKAddress): Gen[OrderMeta] =
    for {
      boxId <- boxIdGen
      ts    <- Gen.choose(1601447470169L, 1603447470169L)
    } yield OrderMeta(boxId, boxValue, address, ts)

  def orderMetaGen(boxValue: Long): Gen[OrderMeta] =
    p2PkAddressGen flatMap (orderMetaGen(boxValue, _))

  def orderMetaGen: Gen[OrderMeta] =
    Gen.posNum[Long] flatMap orderMetaGen

  def bidGen(
    quoteAsset: AssetId,
    baseAsset: AssetId,
    amount: Long,
    price: Long,
    feePerToken: Long
  ): Gen[Order.Bid] =
    orderMetaGen(price * amount + feePerToken * amount) flatMap
    (meta => Order.mkBid(quoteAsset, baseAsset, amount, price, feePerToken, meta))

  def askGen(
    quoteAsset: AssetId,
    baseAsset: AssetId,
    amount: Long,
    price: Long,
    feePerToken: Long
  ): Gen[Order.Ask] =
    orderMetaGen(price * feePerToken) flatMap
    (meta => Order.mkAsk(quoteAsset, baseAsset, amount, price, feePerToken, meta))
}
