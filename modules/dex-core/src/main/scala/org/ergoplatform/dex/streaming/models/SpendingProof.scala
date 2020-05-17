package org.ergoplatform.dex.streaming.models

import cats.instances.either._
import cats.syntax.option._
import io.circe.refined._
import io.circe.{Decoder, DecodingFailure, HCursor, Json}
import org.ergoplatform.dex.HexString

/** A model mirroring SpendingProof entity from Ergo node REST API.
  * See `SpendingProof` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
final case class SpendingProof(proofBytes: Option[HexString], extension: Json)

object SpendingProof {

  implicit val decoder: Decoder[SpendingProof] = { c: HCursor =>
    for {
      // here decoding of refined type field value has to be handled manually as node API
      // may return an empty string (instead of `null`) which fails the refinement.
      proofBytes <- c.downField("proofBytes").as[String].flatMap { s =>
                      // todo: Simplify when node API is improved.
                      HexString.fromString[Either[Throwable, *]](s) match {
                        case Left(_) => Right[DecodingFailure, Option[HexString]](none)
                        case r @ Right(_) =>
                          r.asInstanceOf[Decoder.Result[HexString]].map(_.some)
                      }
                    }
      extension  <- c.downField("extension").as[Json]
    } yield SpendingProof(proofBytes, extension)
  }
}
