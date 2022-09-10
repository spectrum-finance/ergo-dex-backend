package org.ergoplatform.dex.executor.amm

import cats.effect.IO
import fs2.Stream

package object services {

  type StreamF[+A] = Stream[IO, A]
}
