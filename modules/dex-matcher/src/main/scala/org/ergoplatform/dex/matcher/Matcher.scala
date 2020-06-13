package org.ergoplatform.dex.matcher

import fs2._

trait Matcher[F[_]] {

  def run: Stream[F, Unit]
}

object Matcher {

  final private class Live[F[_]] extends Matcher[F] {

    def run: Stream[F, Unit] =
      ??? // read order from the topic -> add to OB -> persist unless filled
  }
}
