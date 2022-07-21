package org.ergoplatform.dex.executor.amm

import cats.effect.IO
import tofu.generate.GenRandom
import tofu.lift.IsoK

import scala.util.Random

package object utils {

  object genRandoms {

    implicit val genRandom: GenRandom[IO] = new GenRandom[IO] {

      override def nextLong: IO[Long] =
        IO.delay(Random.nextLong())

      override def nextInt(n: Int): IO[Int] =
        IO.delay(Random.nextInt(n))
    }
  }

  object isoK {

    implicit val isokIO2IO: IsoK[IO, IO] = new IsoK[IO, IO] {
      override def to[A](fa: IO[A]): IO[A] = fa

      override def from[A](ga: IO[A]): IO[A] = ga
    }
  }
}
