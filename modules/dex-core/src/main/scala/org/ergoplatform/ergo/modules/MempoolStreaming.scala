package org.ergoplatform.ergo.modules

import cats.{Functor, Monad}
import derevo.derive
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.services.node.ErgoNode
import tofu.higherKind.derived.representableK
import tofu.syntax.monadic._

@derive(representableK)
trait MempoolStreaming[F[_]] {

  def streamUnspentOutputs: F[Output]
}

object MempoolStreaming {

  final class Live[F[_]: Functor, G[_]: Monad](node: ErgoNode[G]) extends MempoolStreaming[F] {
    def streamUnspentOutputs: F[Output] = ???
  }
}
