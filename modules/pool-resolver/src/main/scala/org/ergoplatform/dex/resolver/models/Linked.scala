package org.ergoplatform.dex.resolver.models

import derevo.cats.show
import derevo.derive
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.state.Traced
import scodec.Codec
import scodec.codecs._
import tofu.logging.derivation.loggable

@derive(loggable, show)
final case class Linked[S](state: S, predecessorBoxId: Option[BoxId])

object Linked {

  implicit def codec[T: Codec]: Codec[Linked[T]] =
    (implicitly[Codec[T]] :: optional(bool, implicitly[Codec[BoxId]])).as[Linked[T]]
}
