package org.ergoplatform.http.cache

import cats.effect.{IO, Sync}
import cats.syntax.show._
import org.ergoplatform.common.http.cache.HttpResponseCaching.fromResponse
import org.ergoplatform.common.http.cache.models.CachedResponse.toResponse
import org.ergoplatform.http.cache.generators.responseGen
import org.http4s.Response
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scodec.bits.ByteVector
import scorex.util.encode.Base16
import tofu.syntax.monadic._

class HttpCacheSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks {

  property("encode/decode") {

    def showResponse[F[_]: Sync](r: Response[F]): F[String] =
      r.body.compile.to(ByteVector).map { bytes =>
        s"Response(status=${r.status.show}, ${r.httpVersion.show}, ${r.headers.show}, ${Base16.encode(bytes.toArray)})"
      }

    forAll(responseGen[IO]) { re =>
      (for {
        sp   <- fromResponse[IO](re)
        str1 <- showResponse(toResponse[IO](sp))
        str2 <- showResponse(re)
      } yield str1 shouldBe str2).unsafeRunSync()
    }
  }
}
