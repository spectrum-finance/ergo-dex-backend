package org.ergoplatform.dex.streaming

import io.circe.Decoder
import org.ergoplatform.dex.domain.ErgoBox

object schemaCodecs {

  implicit def outputDecoder: Decoder[ErgoBox] = ???
}
