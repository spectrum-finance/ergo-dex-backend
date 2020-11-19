package org.ergoplatform.dex.protocol.models

import cats.effect.Sync
import fs2.kafka.RecordDeserializer
import fs2.kafka.instances.deserializerByDecoder
import io.circe.Decoder
import org.ergoplatform.dex.TxId

/** A model mirroring ErgoTransaction entity from Ergo node REST API.
  * See `ErgoTransaction` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class Transaction(
  id: TxId,
  inputs: List[Input],
  outputs: List[Output],
  size: Int
)

object Transaction {

  implicit val decoder: Decoder[Transaction] = io.circe.derivation.deriveDecoder

  implicit def recordDeserializer[F[_]: Sync]: RecordDeserializer[F, Transaction] = deserializerByDecoder
}
