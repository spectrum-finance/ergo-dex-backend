package org.ergoplatform.dex.domain.models

import io.circe.{Decoder, HCursor}
import org.ergoplatform.dex.BoxId

/** A model mirroring ErgoTransactionInput entity from Ergo node REST API.
  * See `ErgoTransactionInput` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class Input(boxId: BoxId, spendingProof: SpendingProof)

object Input {

  implicit val decoder: Decoder[Input] = { c: HCursor =>
    for {
      boxId         <- c.downField("boxId").as[BoxId]
      spendingProof <- c.downField("spendingProof").as[SpendingProof]
    } yield Input(boxId, spendingProof)
  }
}
