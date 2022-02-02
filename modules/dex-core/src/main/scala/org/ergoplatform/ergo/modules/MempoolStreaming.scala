package org.ergoplatform.ergo.modules

import cats.{Functor, Monad}
import derevo.derive
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.services.node.ErgoNode
import org.ergoplatform.ergo.services.node.models.Transaction
import tofu.higherKind.derived.representableK
import tofu.streams.Evals
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

@derive(representableK)
trait MempoolStreaming[F[_]] {

  def streamUnspentOutputs: F[Output]
}

object MempoolStreaming {

  val Limit = 100

  def make[F[_]: Functor: Evals[*[_], G], G[_]: Monad](implicit node: ErgoNode[G]): MempoolStreaming[F] =
    new Live(node)

  final class Live[F[_]: Functor: Evals[*[_], G], G[_]: Monad](node: ErgoNode[G]) extends MempoolStreaming[F] {

    def streamUnspentOutputs: F[Output] = {
      def fetchMempool(offset: Int, acc: Vector[Transaction]): G[Vector[Transaction]] =
        node.unconfirmedTransactions(offset, Limit) >>=
          (txs => if (txs.size < Limit) txs.pure[G] else fetchMempool(offset + Limit, acc ++ txs))
      evals(fetchMempool(0, Vector.empty).map(extractUtxos))
    }

    private def extractUtxos(txs: Vector[Transaction]): Vector[Output] = {
      val spent   = txs.flatMap(_.inputs.map(_.boxId)).toSet
      val unspent = txs.flatMap(_.outputs).filterNot(o => spent.contains(o.boxId))
      unspent.map(Output.fromNode)
    }
  }
}
