package org.ergoplatform.dex.domain

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.models.Output
import scodec.Codec
import scodec.codecs._
import sttp.tapir.{Schema, Validator}
import tofu.logging.derivation.loggable

@derive(show, encoder, decoder, loggable)
final case class BoxInfo(boxId: BoxId, value: Long, lastConfirmedBoxGix: Long)

object BoxInfo {

  def fromBox(box: Output): BoxInfo =
    BoxInfo(box.boxId, box.value, box.globalIndex)

  implicit val schema: Schema[BoxInfo]       = Schema.derived[BoxInfo]
  implicit val validator: Validator[BoxInfo] = schema.validator

  implicit val codec: Codec[BoxInfo] = (implicitly[Codec[BoxId]] :: int64 :: int64).as[BoxInfo]
}
