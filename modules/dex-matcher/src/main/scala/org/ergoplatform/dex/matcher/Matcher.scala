package org.ergoplatform.dex.matcher

import fs2._
import org.ergoplatform.dex.matcher.context.HasMatcherContext

trait Matcher[F[_]] {

  def run: Stream[F, Unit]
}

object Matcher {

  final private class Live[F[_]: HasMatcherContext] extends Matcher[F] {

    def run: Stream[F, Unit] =
      ??? // read order from the topic -> add to OB -> persist unless filled
  }
}
