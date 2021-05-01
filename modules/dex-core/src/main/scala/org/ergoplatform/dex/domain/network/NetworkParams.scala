package org.ergoplatform.dex.domain.network

import derevo.circe.decoder
import derevo.derive

@derive(decoder)
final case class NetworkParams(minValuePerByte: Long)
