package org.ergoplatform.ergo.state

import derevo.cats.show
import derevo.derive
import org.ergoplatform.ergo.BoxId
import scodec.Codec
import scodec.codecs._
import tofu.logging.derivation.loggable

@derive(loggable, show)
final case class PredictionLink[S](state: PredictedIndexed[S], predecessorBoxId: Option[BoxId])

object PredictionLink {

  implicit def codec[T: Codec]: Codec[PredictionLink[T]] =
    (implicitly[Codec[PredictedIndexed[T]]] :: optional(bool, implicitly[Codec[BoxId]])).as[PredictionLink[T]]
}
