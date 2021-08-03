package org.ergoplatform.dex.executor.amm

import fs2.kafka.types.KafkaOffset
import org.ergoplatform.common.streaming.{Consumer, Delayed, Producer, RetryConsumer}
import org.ergoplatform.dex.domain.amm.{CFMMOrder, OrderId}

object streaming {

  type CFMMConsumerIn[F[_], G[_]]      = Consumer.Aux[OrderId, CFMMOrder, KafkaOffset, F, G]
  type CFMMConsumerRetries[F[_], G[_]] = Consumer.Aux[OrderId, Delayed[CFMMOrder], KafkaOffset, F, G]
  type CFMMProducerRetries[F[_]]       = Producer[OrderId, Delayed[CFMMOrder], F]

  type CFMMConsumer[F[_], G[_]] = RetryConsumer[OrderId, CFMMOrder, F, G]
}
