package org.ergoplatform.dex.executor.amm

import cats.Id
import fs2.kafka.types.KafkaOffset
import org.ergoplatform.common.streaming._
import org.ergoplatform.dex.domain.amm.{CFMMOrder, EvaluatedCFMMOrder, OrderId}

object streaming {

  type CFMMOrdersGen[F[_], G[_], Status[_]] = Consumer.Aux[OrderId, Status[CFMMOrder], KafkaOffset, F, G]
  type CFMMOrders[F[_], G[_]]               = CFMMOrdersGen[F, G, Id]
  type EvaluatedCFMMOrders[F[_], G[_]]      = Consumer.Aux[OrderId, EvaluatedCFMMOrder.Any, KafkaOffset, F, G]

  type CFMMCircuit[F[_], G[_]] = StreamingCircuit[OrderId, CFMMOrder, F, G]
}
