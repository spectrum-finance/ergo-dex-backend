package org.ergoplatform.common.http.cache.models

import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec
import org.http4s.{Header, Headers, HttpVersion, Response, Status}
import org.typelevel.ci.CIString

case class ResponseDB(
  status: Status,
  httpVersion: HttpVersion,
  headers: Headers,
  body: Array[Byte]
)

object ResponseDB {

  implicit val cisEncoder: Encoder[CIString]    = Encoder[String].contramap(_.toString)
  implicit val cisDecoder: Decoder[CIString]    = Decoder[String].map(CIString.apply)
  implicit val rawCodec: Codec[Header.Raw]      = deriveCodec
  implicit val headersCodec: Codec[Headers]     = deriveCodec
  implicit val statusCodec: Codec[Status]       = deriveCodec
  implicit val httpvCodec: Codec[HttpVersion]   = deriveCodec
  implicit val responseCodec: Codec[ResponseDB] = deriveCodec

  def respFromDB[F[_]](resp: ResponseDB): Response[F] =
    Response(
      resp.status,
      resp.httpVersion,
      resp.headers,
      fs2.Stream.emits(resp.body)
    )
}
