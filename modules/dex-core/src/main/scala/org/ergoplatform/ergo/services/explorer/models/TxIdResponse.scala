package org.ergoplatform.ergo.services.explorer.models

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.ergo.TxId

@derive(decoder)
final case class TxIdResponse(id: TxId)
