package org.ergoplatform.dex.tracker

import fs2.kafka.types.KafkaOffset
import org.ergoplatform.common.streaming.Consumer

package object streaming {

  type MempoolConsumer[S[_], F[_]] = Consumer.Aux[String, Option[MempoolEvent], KafkaOffset, S, F]

  type TransactionConsumer[S[_], F[_]] = Consumer.Aux[String, Option[TransactionEvent], KafkaOffset, S, F]
}
