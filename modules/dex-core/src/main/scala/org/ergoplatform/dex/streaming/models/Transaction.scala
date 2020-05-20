package org.ergoplatform.dex.streaming.models

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
