package org.ergoplatform.dex.matcher

import fs2._

abstract class Matcher[F[_], S[_[_] <: F[_], _]] {

  def run: S[F, Unit]
}

object Matcher {

  final private class Live[F[_]] extends Matcher[F, Stream] {

    def run: Stream[F, Unit] = ??? // read order from the topic -> add to OB -> persist unless filled
  }
}
