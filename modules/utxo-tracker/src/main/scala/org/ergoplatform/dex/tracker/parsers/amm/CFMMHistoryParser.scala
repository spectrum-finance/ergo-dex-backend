package org.ergoplatform.dex.tracker.parsers.amm

import cats.Monad
import cats.instances.list._
import cats.syntax.option._
import cats.syntax.traverse._
import org.ergoplatform.dex.domain.amm.OrderEvaluation.{DepositEvaluation, RedeemEvaluation, SwapEvaluation}
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.ergo.domain.{Output, SettledTransaction}
import tofu.higherKind.Embed
import tofu.syntax.foption._
import tofu.syntax.monadic._

trait CFMMHistoryParser[+CT <: CFMMType, F[_]] {

  def swap(tx: SettledTransaction): F[Option[EvaluatedCFMMOrder[CFMMVersionedOrder.AnySwap, SwapEvaluation]]]

  def deposit(tx: SettledTransaction): F[Option[EvaluatedCFMMOrder[CFMMVersionedOrder.AnyDeposit, DepositEvaluation]]]

  def redeem(tx: SettledTransaction): F[Option[EvaluatedCFMMOrder[CFMMVersionedOrder.AnyRedeem, RedeemEvaluation]]]
}

object CFMMHistoryParser {

  def apply[CT <: CFMMType, F[_]](implicit ev: CFMMHistoryParser[CT, F]): CFMMHistoryParser[CT, F] = ev

  implicit def embed[CT <: CFMMType]: Embed[CFMMHistoryParser[CT, *[_]]] = {
    type Rep[F[_]] = CFMMHistoryParser[CT, F]
    tofu.higherKind.derived.genEmbed[Rep]
  }

  implicit def t2tCFMMHistory[F[_]: Monad](implicit
    orders: CFMMParser[T2T_CFMM, F],
    pools: CFMMPoolsParser[T2T_CFMM],
    evals: CFMMOrderEvaluationParser[F],
                                           orderExecutorFee: OrderExecutorFeeParser
  ): CFMMHistoryParser[T2T_CFMM, F] =
    new UniversalParser[T2T_CFMM, F]

  implicit def n2tCFMMHistory[F[_]: Monad](implicit
    orders: CFMMParser[N2T_CFMM, F],
    pools: CFMMPoolsParser[N2T_CFMM],
    evals: CFMMOrderEvaluationParser[F],
                                           orderExecutorFee: OrderExecutorFeeParser
  ): CFMMHistoryParser[N2T_CFMM, F] =
    new UniversalParser[N2T_CFMM, F]

  final class UniversalParser[+CT <: CFMMType, F[_]: Monad](implicit
    orders: CFMMParser[CT, F],
    pools: CFMMPoolsParser[CT],
    evals: CFMMOrderEvaluationParser[F],
                                                            orderExecutorFee: OrderExecutorFeeParser
  ) extends CFMMHistoryParser[CT, F] {

    def swap(tx: SettledTransaction): F[Option[EvaluatedCFMMOrder[CFMMVersionedOrder.AnySwap, SwapEvaluation]]] =
      parseSomeOrder(tx)(orders.swap, (o, _, a: CFMMVersionedOrder.AnySwap) => evals.parseSwapEval(o, a))
        .mapIn {
          case x @ EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapV0, _, _, _) =>
            x.copy(order = o.copy(timestamp = tx.timestamp))
          case x @ EvaluatedCFMMOrder(o: CFMMVersionedOrder.SwapV1, _, _, _) =>
            x.copy(order = o.copy(timestamp = tx.timestamp))
        }

    def deposit(
      tx: SettledTransaction
    ): F[Option[EvaluatedCFMMOrder[CFMMVersionedOrder.AnyDeposit, DepositEvaluation]]] =
      parseSomeOrder(tx)(orders.deposit, evals.parseDepositEval)
        .mapIn {
          case x@EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV0, _, _, _) =>
            x.copy(order = o.copy(timestamp = tx.timestamp))
          case x@EvaluatedCFMMOrder(o: CFMMVersionedOrder.DepositV1, _, _, _) =>
            x.copy(order = o.copy(timestamp = tx.timestamp))
        }

    def redeem(tx: SettledTransaction): F[Option[EvaluatedCFMMOrder[CFMMVersionedOrder.AnyRedeem, RedeemEvaluation]]] =
      parseSomeOrder(tx)(orders.redeem, evals.parseRedeemEval)
        .mapIn {
          case x @ EvaluatedCFMMOrder(o: CFMMVersionedOrder.RedeemV0, _, _, _) =>
            x.copy(order = o.copy(timestamp = tx.timestamp))
          case x @ EvaluatedCFMMOrder(o: CFMMVersionedOrder.RedeemV1, _, _, _) =>
            x.copy(order = o.copy(timestamp = tx.timestamp))
        }

    private def parseSomeOrder[A <: CFMMVersionedOrder.Any, E <: OrderEvaluation](
      tx: SettledTransaction
    )(
      opParser: Output => F[Option[A]],
      evalParse: (Output, CFMMPool, A) => F[Option[E]]
    ): F[Option[EvaluatedCFMMOrder[A, E]]] = {
      val inputs = tx.tx.inputs.map(_.output)
      def parseExecutedOrder(order: A): F[Option[EvaluatedCFMMOrder[A, E]]] = {
        def parseOrderExecutorFee(pool: CFMMPool): Option[OrderExecutorFee] =
          tx.tx.outputs.map(orderExecutorFee.parse(order, _, tx.timestamp, pool)).collectFirst { case Some(v) => v }

        inputs.map(pools.pool).collectFirst { case Some(p) => p } match {
          case Some(p) =>
            tx.tx.outputs
              .traverse(o => evalParse(o, p, order))
              .map(_.collectFirst { case Some(c) => c })
              .map(eval => EvaluatedCFMMOrder(order, eval, p.some, parseOrderExecutorFee(p)).some)
          case None => EvaluatedCFMMOrder(order, none, none, none).someF[F]
        }
      }

      inputs
        .traverse(opParser)
        .map(_.collectFirst { case Some(x) => x })
        .flatMap(_.fold(noneF[F, EvaluatedCFMMOrder[A, E]])(parseExecutedOrder))
    }
  }
}
