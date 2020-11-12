package org.ergoplatform.dex.streaming

import org.ergoplatform.dex.configs.ProducerConfig
import fs2._
import fs2.kafka.ProducerRecords

trait MakeKafkaProducer[F[_], K, V] {

  def apply(config: ProducerConfig): Pipe[F, ProducerRecords[K, V, Unit], Unit]
}

object MakeKafkaProducer {


}
