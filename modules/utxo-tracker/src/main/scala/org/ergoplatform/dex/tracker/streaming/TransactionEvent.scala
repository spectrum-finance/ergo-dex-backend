package org.ergoplatform.dex.tracker.streaming

import cats.effect.Sync
import derevo.derive
import fs2.kafka.{Deserializer, RecordDeserializer}
import io.circe.parser.decode
import org.ergoplatform.ErgoLikeTransactionSerializer
import org.ergoplatform.dex.tracker.domain.Transaction
import scorex.util.encode.Base64
import tofu.logging.derivation.loggable

import scala.util.Try

@derive(loggable)
sealed trait TransactionEvent {
  val transaction: Transaction
  val timestamp: Long
  val height: Int
}

object TransactionEvent {

  @derive(loggable)
  final case class TransactionApply(transaction: Transaction, timestamp: Long, height: Int) extends TransactionEvent

  @derive(loggable)
  final case class TransactionUnapply(transaction: Transaction, timestamp: Long, height: Int) extends TransactionEvent

  implicit def transactionEventDeserializer[F[_]: Sync]: RecordDeserializer[F, Option[TransactionEvent]] =
    RecordDeserializer.lift(Deserializer.string.map { str =>
      decode[KafkaTxEvent](str).toOption.flatMap(fromKafkaEvent)
    })

  private def fromKafkaEvent(event: KafkaTxEvent): Option[TransactionEvent] =
    Base64.decode(event.tx).flatMap(b => Try(ErgoLikeTransactionSerializer.fromBytes(b))).toOption.map { tx =>
      event match {
        case KafkaTxEvent.AppliedEvent(timestamp, _, height) =>
          TransactionApply(Transaction.fromErgoLike(tx), timestamp, height)
        case KafkaTxEvent.UnappliedEvent(_) => TransactionUnapply(Transaction.fromErgoLike(tx), 0, 0)
      }
    }
}
