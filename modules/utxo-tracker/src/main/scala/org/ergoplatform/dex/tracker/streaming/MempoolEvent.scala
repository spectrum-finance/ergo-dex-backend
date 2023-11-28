package org.ergoplatform.dex.tracker.streaming

import cats.effect.Sync
import derevo.circe.decoder
import derevo.derive
import fs2.kafka.{Deserializer, RecordDeserializer}
import io.circe.parser.decode
import org.ergoplatform.ErgoLikeTransactionSerializer
import scorex.util.encode.Base64
import cats.syntax.either._
import org.ergoplatform.dex.tracker.domain.Transaction

import scala.util.Try

@derive(decoder)
sealed trait MempoolEvent {
  val transaction: Transaction
}

object MempoolEvent {

  @derive(decoder)
  final case class MempoolApply(transaction: Transaction) extends MempoolEvent

  @derive(decoder)
  final case class MempoolUnapply(transaction: Transaction) extends MempoolEvent

  implicit def mempoolEventDeserializer[F[_]: Sync]: RecordDeserializer[F, Option[MempoolEvent]] =
    RecordDeserializer.lift(Deserializer.string.attempt.map { str =>
      str
        .flatMap(decode[KafkaMempoolEvent](_))
        .toOption
        .flatMap(fromKafkaEvent)
    })

  def fromKafkaEvent(event: KafkaMempoolEvent): Option[MempoolEvent] =
    Base64
      .decode(event.tx)
      .flatMap { b =>
        Try(ErgoLikeTransactionSerializer.fromBytes(b))
      }
      .toOption
      .map { tx =>
        event match {
          case KafkaMempoolEvent.TxAccepted(_)  => MempoolApply(Transaction.fromErgoLike(tx))
          case KafkaMempoolEvent.TxWithdrawn(_) => MempoolUnapply(Transaction.fromErgoLike(tx))
        }
      }
}