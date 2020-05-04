package org.ergoplatform.dex.watcher.streaming

import io.circe.Decoder
import org.ergoplatform.dex.watcher.domain.ErgoBox

object schemaCodecs {

  implicit def outputDecoder: Decoder[ErgoBox] = ???
}
