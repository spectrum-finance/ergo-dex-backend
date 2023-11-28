package org.ergoplatform.dex.tracker.streaming

import derevo.circe.{decoder, encoder}
import derevo.derive

@derive(encoder, decoder)
sealed trait KafkaMempoolEvent {
  val tx: String
}

object KafkaMempoolEvent {

  @derive(encoder, decoder)
  final case class TxAccepted(tx: String) extends KafkaMempoolEvent

  @derive(encoder, decoder)
  final case class TxWithdrawn(tx: String) extends KafkaMempoolEvent
}
