package org.ergoplatform.dex.index

import fs2.kafka.types.KafkaOffset
import org.ergoplatform.common.streaming.Consumer
import org.ergoplatform.dex.domain.amm.{CFMMOrder, OrderId}

object streaming {
  type CFMMConsumer[F[_], G[_]] = Consumer.Aux[OrderId, CFMMOrder, KafkaOffset, F, G]
}
