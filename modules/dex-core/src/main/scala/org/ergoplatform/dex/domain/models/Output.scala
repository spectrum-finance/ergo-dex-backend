package org.ergoplatform.dex.domain.models

import io.circe.refined._
import io.circe.{Decoder, HCursor, Json}
import org.ergoplatform.dex.{BoxId, HexString}

/** A model mirroring ErgoTransactionOutput entity from Ergo node REST API.
  * See `ErgoTransactionOutput` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class Output(
  boxId: BoxId,
  value: Long,
  creationHeight: Int,
  ergoTree: HexString,
  assets: List[Asset],
  additionalRegisters: Json
)

object Output {

  implicit val decoder: Decoder[Output] = { c: HCursor =>
    for {
      boxId               <- c.downField("boxId").as[BoxId]
      value               <- c.downField("value").as[Long]
      creationHeight      <- c.downField("creationHeight").as[Int]
      ergoTree            <- c.downField("ergoTree").as[HexString]
      assets              <- c.downField("assets").as[List[Asset]]
      additionalRegisters <- c.downField("additionalRegisters").as[Json]
    } yield Output(boxId, value, creationHeight, ergoTree, assets, additionalRegisters)
  }
}
