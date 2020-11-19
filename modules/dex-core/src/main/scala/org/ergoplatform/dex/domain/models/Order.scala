package org.ergoplatform.dex.domain.models

import cats.Show
import cats.effect.Sync
import cats.syntax.semigroup._
import cats.syntax.show._
import doobie.util.Write
import fs2.kafka.{RecordDeserializer, RecordSerializer}
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import io.estatico.newtype.ops._
import org.ergoplatform.dex.{AssetId, OrderId, PairId}
import org.ergoplatform.dex.protocol.instances._
import tofu.logging.{Loggable, _}

/** Global market order.
  * @param `type` - type of the order (sell or buy)
  * @param quoteAsset  - id of the quote asset
  * @param baseAsset - id of the base asset
  * @param amount - amount of `quoteAsset`
  * @param price - amount of `baseAsset` for one unit of `quoteAsset`
  * @param feePerToken - amount of fee (in nanoERG) per one traded `quoteAsset`
  * @param meta - order metadata
  */
final case class Order[T <: OrderType](
  `type`: T,
  quoteAsset: AssetId,
  baseAsset: AssetId,
  amount: Long,
  price: Long,
  feePerToken: Long,
  meta: OrderMeta
) {

  def id: OrderId = meta.boxId.value.coerce[OrderId]

  def fee: Long = feePerToken * amount

  def pairId: PairId = PairId(quoteAsset, baseAsset)
}

object Order {

  type Ask      = Order[OrderType.Ask.type]
  type Bid      = Order[OrderType.Bid.type]
  type AnyOrder = Order[_ <: OrderType]

  implicit val write: Write[AnyOrder] = Write[Ask].asInstanceOf[Write[AnyOrder]]

  implicit def show[T <: OrderType]: Show[Order[T]] =
    o =>
      s"Order[${OrderType.show.show(o.`type`)}](quoteAsset=${o.quoteAsset}, baseAsset=${o.baseAsset}, " +
      s"amount=${o.amount}, price=${o.price}, feePerToken=${o.feePerToken}, meta=${o.meta.show})"

  implicit def loggable[T <: OrderType]: Loggable[Order[T]] =
    new DictLoggable[Order[T]] {

      override def fields[I, V, R, S](a: Order[T], i: I)(implicit r: LogRenderer[I, V, R, S]): R =
        r.addString("type", OrderType.show.show(a.`type`), i) |+|
        r.addString("quoteAsset", a.quoteAsset.unwrapped, i) |+|
        r.addString("baseAsset", a.baseAsset.unwrapped, i) |+|
        r.addBigInt("amount", a.amount, i) |+|
        r.addBigInt("quoteAsset", a.price, i) |+|
        r.addBigInt("feePerToken", a.feePerToken, i)

      override def logShow(a: Order[T]): String = a.show
    }

  implicit val encoder: Encoder[AnyOrder] =
    io.circe.derivation.deriveEncoder[Order[OrderType]].asInstanceOf[Encoder[AnyOrder]]

  implicit val decoder: Decoder[AnyOrder] =
    io.circe.derivation.deriveDecoder[Order[OrderType]].asInstanceOf[Decoder[AnyOrder]]

  implicit def recordSerializer[F[_]: Sync]: RecordSerializer[F, AnyOrder] =
    fs2.kafka.instances.serializerFromEncoder

  implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, AnyOrder] =
    fs2.kafka.instances.deserializerFromDecoder

  def mkBid(
    quoteAsset: AssetId,
    baseAsset: AssetId,
    amount: Long,
    price: Long,
    feePerToken: Long,
    meta: OrderMeta
  ): Bid =
    Order(OrderType.Bid, quoteAsset, baseAsset, amount, price, feePerToken, meta)

  def mkAsk(
    quoteAsset: AssetId,
    baseAsset: AssetId,
    amount: Long,
    price: Long,
    feePerToken: Long,
    meta: OrderMeta
  ): Ask =
    Order(OrderType.Ask, quoteAsset, baseAsset, amount, price, feePerToken, meta)
}
