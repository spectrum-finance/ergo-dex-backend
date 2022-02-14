package org.ergoplatform.ergo.services.node.models

import derevo.circe.decoder
import derevo.derive

@derive(decoder)
final case class ApiError(error: Int, reason: String, detail: String)
