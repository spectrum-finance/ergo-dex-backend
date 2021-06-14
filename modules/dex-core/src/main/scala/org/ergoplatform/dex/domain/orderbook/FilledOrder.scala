package org.ergoplatform.dex.domain.orderbook

import cats.Show
import cats.syntax.show._
import io.circe.{Decoder, Encoder}
import org.ergoplatform.dex.protocol.instances._
import tofu.logging.{Loggable, _}

final case class FilledOrder[+T <: OrderType](
  base: Order[T],
  executionPrice: Long
)

object FilledOrder {

  type FilledAsk      = FilledOrder[OrderType.Ask.type]
  type FilledBid      = FilledOrder[OrderType.Bid.type]
  type AnyFilledOrder = FilledOrder[OrderType]

  implicit def show[T <: OrderType]: Show[FilledOrder[T]] =
    o => s"FilledOrder[${OrderType.show.show(o.base.`type`)}](base=${o.base.show}, executionPrice=${o.executionPrice})"

  implicit def loggable[T <: OrderType]: Loggable[FilledOrder[T]] =
    new DictLoggable[FilledOrder[T]] {

      override def fields[I, V, R, S](a: FilledOrder[T], i: I)(implicit r: LogRenderer[I, V, R, S]): R =
        r.addBigInt("executionPrice", a.executionPrice, i)

      override def logShow(a: FilledOrder[T]): String = a.show
    }

  implicit val encoder: Encoder[AnyFilledOrder] =
    io.circe.derivation.deriveEncoder[AnyFilledOrder]

  implicit val decoder: Decoder[AnyFilledOrder] =
    io.circe.derivation.deriveDecoder[AnyFilledOrder]
}
