package fs2.kafka

import cats.data.Chain
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.ergoplatform.dex.streaming.Committable

object KafkaCommittable {

  def apply[F[_], K, V](
    committable: CommittableConsumerRecord[F, K, V]
  ): Committable[V, (TopicPartition, OffsetAndMetadata), F] =
    new Committable[V, (TopicPartition, OffsetAndMetadata), F] {
      def message: V = committable.record.value

      def offset: (TopicPartition, OffsetAndMetadata) =
        committable.offset.topicPartition -> committable.offset.offsetAndMetadata

      def commit: ((TopicPartition, OffsetAndMetadata)) => F[Unit] = offset => batchCommit(Chain.one(offset))

      def batchCommit: Chain[(TopicPartition, OffsetAndMetadata)] => F[Unit] = offsets =>
        committable.offset.commitOffsets(offsets.toList.toMap)
    }
}
