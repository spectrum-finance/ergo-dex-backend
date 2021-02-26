package org.ergoplatform.dex.clients.explorer.models

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.dex.TxId

@derive(decoder)
final case class TxIdResponse(id: TxId)
