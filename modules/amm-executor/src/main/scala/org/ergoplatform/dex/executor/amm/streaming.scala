package org.ergoplatform.dex.executor.amm

import fs2.kafka.types.KafkaOffset
import org.ergoplatform.common.streaming._
import org.ergoplatform.dex.domain.amm.{CFMMOrder, OrderId}

object streaming {

  type CFMMConsumerIn[F[_], G[_]]      = Consumer.Aux[OrderId, CFMMOrder, KafkaOffset, F, G]
  type CFMMConsumerRetries[F[_], G[_]] = Consumer.Aux[OrderId, Delayed[CFMMOrder], KafkaOffset, F, G]
  type CFMMProducerRetries[F[_]]       = Producer[OrderId, Delayed[CFMMOrder], F]

  type CFMMCircuit[F[_], G[_]] = StreamingCircuit[OrderId, CFMMOrder, F, G]
}
