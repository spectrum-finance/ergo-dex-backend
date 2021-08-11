package org.ergoplatform.dex.domain.amm.state

import derevo.cats.show
import derevo.derive
import org.ergoplatform.ergo.BoxId
import scodec.Codec
import scodec.codecs._
import tofu.logging.derivation.loggable

@derive(loggable, show)
final case class PredictionLink[S](state: Predicted[S], predecessorBoxId: Option[BoxId])

object PredictionLink {

  implicit def codec[T: Codec]: Codec[PredictionLink[T]] =
    (implicitly[Codec[Predicted[T]]] :: optional(bool, implicitly[Codec[BoxId]])).as[PredictionLink[T]]
}
