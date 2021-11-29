package org.ergoplatform.dex.index.db

import doobie.util.Put
import doobie.{Get, Meta}
import io.circe.Json
import io.circe.parser.parse
import org.postgresql.util.PGobject

object instances {

  implicit val getBigInt: Get[BigInt] = implicitly[Get[BigDecimal]].temap(x =>
    x.toBigIntExact.fold[Either[String, BigInt]](Left(s"Failed to convert '$x' to BigInt"))(Right(_))
  )

  implicit val putBigInt: Put[BigInt] = implicitly[Put[BigDecimal]].contramap[BigInt](BigDecimal(_))

  implicit val metaBigInt: Meta[BigInt] = new Meta(getBigInt, putBigInt)

  implicit val jsonMeta: Meta[Json] =
    Meta.Advanced
      .other[PGobject]("json")
      .imap[Json](a => parse(a.getValue).right.getOrElse(Json.Null))(mkPgJson)

  private def mkPgJson(a: Json) = {
    val o = new PGobject
    o.setType("json")
    o.setValue(a.noSpaces)
    o
  }
}
