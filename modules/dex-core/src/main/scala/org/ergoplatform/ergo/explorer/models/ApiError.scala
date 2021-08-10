package org.ergoplatform.ergo.explorer.models

import derevo.circe.decoder
import derevo.derive

@derive(decoder)
final case class ApiError(status: Int, reason: String)
