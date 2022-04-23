package org.ergoplatform.http.cache

import org.http4s.Header.Raw
import org.http4s.{Headers, HttpVersion, Response, Status}
import org.scalacheck.Gen
import org.typelevel.ci.CIString
import fs2._

object generators {

  def statusGen: Gen[Status] =
    Gen.oneOf(
      Status(200),
      Status(401),
      Status(402),
      Status(403),
      Status(404),
      Status(500),
      Status(501),
      Status(502),
      Status(503)
    )

  def httpGen: Gen[HttpVersion] =
    Gen.oneOf(
      HttpVersion.`HTTP/1.1`,
      HttpVersion.`HTTP/1.0`,
      HttpVersion.`HTTP/2.0`
    )

  def headersGen: Gen[Headers] =
    Gen.oneOf(
      Headers(List(Raw(CIString("key"), "value"))),
      Headers(List(Raw(CIString("auth"), "password"))),
      Headers(List(Raw(CIString("Content-Type"), "Application-Json")))
    )

  def responseGen[F[_]]: Gen[Response[F]] =
    for {
      status      <- statusGen
      httpVersion <- httpGen
      headers     <- headersGen
      body        <- Gen.listOfN(32, Gen.posNum[Byte]).map(_.toArray)
    } yield Response(status, httpVersion, headers, Stream.emits(body))
}
