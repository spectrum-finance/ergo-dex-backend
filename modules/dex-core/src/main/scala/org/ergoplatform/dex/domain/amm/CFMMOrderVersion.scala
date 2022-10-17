package org.ergoplatform.dex.domain.amm

import doobie.util.{Get, Put}
import cats.syntax.either._

sealed trait CFMMOrderVersion

object CFMMOrderVersion {
  sealed trait V0 extends CFMMOrderVersion
  sealed trait V1 extends CFMMOrderVersion

  def v0: V0 = new V0 {}
  def v1: V1 = new V1 {}

  implicit def put: Put[CFMMOrderVersion] = Put[Int].contramap {
    case _: V0 => 0
    case _: V1 => 1
  }

  implicit def get: Get[CFMMOrderVersion] = Get[Int].temap {
    case 0   => v0.asRight[String]
    case 1   => v1.asRight[String]
    case err => s"Invalid contract version: $err".asLeft[CFMMOrderVersion]
  }
}
