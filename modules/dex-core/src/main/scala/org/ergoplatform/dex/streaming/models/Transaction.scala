package org.ergoplatform.dex.streaming.models

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

  implicit val decoder: Decoder[Transaction] = { c =>
    for {
      id      <- c.downField("id").as[TxId]
      inputs  <- c.downField("inputs").as[List[Input]]
      outputs <- c.downField("outputs").as[List[Output]]
      size    <- c.downField("size").as[Int]
    } yield Transaction(id, inputs, outputs, size)
  }
}
