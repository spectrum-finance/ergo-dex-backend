package org.ergoplatform.common.http.cache.models

import cats.implicits._
import _root_.scodec.interop.cats._
import _root_.scodec._
import _root_.scodec.codecs._
import derevo.derive
import org.http4s._
import fs2._
import org.typelevel.ci._
import org.typelevel.vault.Vault
import scodec.bits.ByteVector
import tofu.logging.Loggable
import tofu.logging.derivation.loggable
import java.nio.charset.StandardCharsets

@derive(loggable)
case class CachedResponse(
  status: Status,
  httpVersion: HttpVersion,
  headers: Headers,
  body: ByteVector
)

object CachedResponse {

  implicit val loggableByteVector: Loggable[ByteVector] = Loggable.show

  implicit val loggableStatus: Loggable[Status] = Loggable.show

  implicit val statusCodec: Codec[Status] = int16.exmap(
    i => Attempt.fromEither(Status.fromInt(i).leftMap(p => Err.apply(p.details))),
    s => Attempt.successful(s.code)
  )

  implicit val loggableHttpVersion: Loggable[HttpVersion] = Loggable.show

  implicit val httpVersionCodec: Codec[HttpVersion] = {
    def decode(major: Int, minor: Int): Attempt[HttpVersion] =
      Attempt.fromEither(HttpVersion.fromVersion(major, minor).leftMap(p => Err.apply(p.message)))
    (int8 ~ int8).exmap(
      decode,
      httpVersion => Attempt.successful(httpVersion.major -> httpVersion.minor)
    )
  }

  implicit val loggableHeaders: Loggable[Headers] = Loggable.show

  implicit val headersCodec: Codec[Headers] =
    string32(StandardCharsets.UTF_8).exmapc { s =>
      if (s.isEmpty())
        Attempt.successful(Headers.empty)
      else
        s.split("\r\n")
          .toList
          .traverse { line =>
            val idx = line.indexOf(':')
            if (idx >= 0) {
              Attempt.successful(Header.Raw(CIString(line.substring(0, idx)), line.substring(idx + 1).trim))
            } else Attempt.failure[Header.Raw](Err(s"No : found in Header - $line"))
          }
          .map(Headers(_))

    } { h =>
      Attempt.successful(
        h.headers
          .map(h => s"${h.name.toString}:${h.value}")
          .intercalate("\r\n")
      )
    }

  implicit val httpDateCodec: Codec[HttpDate] =
    int64.exmapc(i => Attempt.fromEither(HttpDate.fromEpochSecond(i).leftMap(e => Err(e.details))))(date =>
      Attempt.successful(date.epochSecond)
    )

  implicit val loggableMethod: Loggable[Method] = Loggable.show

  implicit val method: Codec[Method] = string32(StandardCharsets.UTF_8).exmapc(s =>
    Attempt.fromEither(Method.fromString(s).leftMap(p => Err.apply(p.details)))
  )(m => Attempt.successful(m.name))

  implicit val loggableUri: Loggable[Uri] = Loggable.show

  implicit val uri: Codec[Uri] = string32(StandardCharsets.UTF_8)
    .exmapc(s => Attempt.fromEither(Uri.fromString(s).leftMap(p => Err.apply(p.details))))(uri =>
      Attempt.successful(uri.renderString)
    )

  implicit val keyTupleCodec: Codec[(Method, Uri)] = method ~ uri

  implicit val cachedResponseCodec: Codec[CachedResponse] =
    (statusCodec :: httpVersionCodec :: headersCodec :: variableSizeBytesLong(int64, bytes)).as[CachedResponse]

  def toResponse[F[_]](resp: CachedResponse): Response[F] =
    Response(
      resp.status,
      resp.httpVersion,
      resp.headers,
      Stream.chunk(Chunk.byteVector(resp.body)),
      Vault.empty
    )
}
