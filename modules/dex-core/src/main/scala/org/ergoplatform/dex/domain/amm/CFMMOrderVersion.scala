package org.ergoplatform.dex.domain.amm

import doobie.util.{Get, Put}
import cats.syntax.either._
import io.circe.{Decoder, Encoder}

sealed trait CFMMOrderVersion

object CFMMOrderVersion {
  sealed trait V0 extends CFMMOrderVersion
  sealed trait V1 extends CFMMOrderVersion
  sealed trait V2 extends CFMMOrderVersion
  sealed trait V3 extends CFMMOrderVersion


  type Any = CFMMOrderVersion

  def v0: V0 = new V0 {}
  def v1: V1 = new V1 {}
  def v2: V2 = new V2 {}
  def v3: V3 = new V3 {}

  implicit val encoder: Encoder[CFMMOrderVersion] = Encoder[Int].contramap {
    case _: V0 => 0
    case _: V1 => 1
    case _: V2 => 2
    case _: V3 => 3
  }

  implicit val decoder: Decoder[CFMMOrderVersion] = Decoder[Int].emap {
    case 0   => v0.asRight
    case 1   => v1.asRight
    case 2   => v2.asRight
    case 3   => v3.asRight
    case err => s"Incorrect version $err".asLeft
  }

  implicit def put: Put[CFMMOrderVersion] = Put[Int].contramap {
    case _: V0 => 0
    case _: V1 => 1
    case _: V2 => 2
    case _: V3 => 3
  }

  implicit def get: Get[CFMMOrderVersion] = Get[Int].temap {
    case 0   => v0.asRight[String]
    case 1   => v1.asRight[String]
    case 2   => v2.asRight[String]
    case 3   => v3.asRight[String]
    case err => s"Invalid contract version: $err".asLeft[CFMMOrderVersion]
  }
}
