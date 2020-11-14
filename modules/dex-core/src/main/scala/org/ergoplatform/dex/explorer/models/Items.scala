package org.ergoplatform.dex.explorer.models

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class Items[A](items: List[A], total: Int)

object Items {

  implicit def decoder[A: Decoder]: Decoder[Items[A]] = deriveDecoder
}
