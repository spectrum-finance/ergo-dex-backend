package org.ergoplatform.dex.executor.amm

import org.ergoplatform.dex.domain.amm.{CFMMOrder, OrderId}
import org.ergoplatform.common.streaming.{Consumer, Delayed, Producer}

object streaming {

  type CFMMConsumer[F[_], G[_]] = Consumer[OrderId, CFMMOrder, F, G]

  type OrdersRotation[F[_], G[_]] = Producer[OrderId, Delayed[CFMMOrder], F]
}
