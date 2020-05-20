package org.ergoplatform.dex.streaming.models

import org.ergoplatform.dex.BoxId

/** A model mirroring ErgoTransactionInput entity from Ergo node REST API.
  * See `ErgoTransactionInput` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class Input(boxId: BoxId, spendingProof: SpendingProof)
