package org.ergoplatform.ergo.services.explorer.models

import derevo.circe.decoder
import derevo.derive

@derive(decoder)
final case class Items[A](items: List[A], total: Int)
