package org.ergoplatform.dex.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.models.Output
import sttp.tapir.{Schema, Validator}
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class BoxInfo(boxId: BoxId, value: Long, lastConfirmedBoxGix: Long)

object BoxInfo {

  def fromBox(box: Output): BoxInfo =
    BoxInfo(box.boxId, box.value, box.globalIndex)

  implicit val schema: Schema[BoxInfo]       = Schema.derived[BoxInfo]
  implicit val validator: Validator[BoxInfo] = schema.validator
}
