package org.ergoplatform.dex.domain.syntax

import cats.syntax.bifunctor._
import cats.instances.tuple._
import org.ergoplatform.dex.domain.models.Order.{AnyOrder, BuyOrder, SellOrder}

object order {

  implicit final class OrdersOps(private val xs: List[AnyOrder]) extends AnyVal {

    def partitioned: (List[SellOrder], List[BuyOrder]) =
      xs.partition(_.`type`.isSell)
        .bimap(_.map(_.asInstanceOf[SellOrder]), _.map(_.asInstanceOf[BuyOrder]))
  }
}
