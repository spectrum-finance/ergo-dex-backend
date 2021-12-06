package org.ergoplatform.dex.tracker.parsers.amm

import cats.Monad
import cats.syntax.traverse._
import cats.syntax.option._
import cats.instances.list._
import org.ergoplatform.dex.domain.amm.OrderEvaluation.{DepositEvaluation, RedeemEvaluation, SwapEvaluation}
import org.ergoplatform.dex.domain.amm.state.Confirmed
import org.ergoplatform.dex.domain.amm.{CFMMOrder, CFMMPool, Deposit, EvaluatedCFMMOrder, OrderEvaluation, Redeem, Swap}
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.ergo.models.{Output, Transaction}
import tofu.higherKind.Embed
import tofu.syntax.monadic._
import tofu.syntax.foption._

trait CFMMHistoryParser[+CT <: CFMMType, F[_]] {

  def swap(tx: Transaction): F[Option[EvaluatedCFMMOrder[Swap, SwapEvaluation]]]

  def deposit(tx: Transaction): F[Option[EvaluatedCFMMOrder[Deposit, DepositEvaluation]]]

  def redeem(tx: Transaction): F[Option[EvaluatedCFMMOrder[Redeem, RedeemEvaluation]]]
}

object CFMMHistoryParser {

  def apply[CT <: CFMMType, F[_]](implicit ev: CFMMHistoryParser[CT, F]): CFMMHistoryParser[CT, F] = ev

  implicit def embed[CT <: CFMMType]: Embed[CFMMHistoryParser[CT, *[_]]] = {
    type Rep[F[_]] = CFMMHistoryParser[CT, F]
    tofu.higherKind.derived.genEmbed[Rep]
  }

  implicit def t2tCFMMHistory[F[_]: Monad](implicit
    orders: CFMMOrdersParser[T2T_CFMM, F],
    pools: CFMMPoolsParser[T2T_CFMM],
    evals: CFMMOrderEvaluationParser[F]
  ): CFMMHistoryParser[T2T_CFMM, F] =
    new UniversalParser[T2T_CFMM, F]

  implicit def n2tCFMMHistory[F[_]: Monad](implicit
    orders: CFMMOrdersParser[N2T_CFMM, F],
    pools: CFMMPoolsParser[N2T_CFMM],
    evals: CFMMOrderEvaluationParser[F]
  ): CFMMHistoryParser[N2T_CFMM, F] =
    new UniversalParser[N2T_CFMM, F]

  final class UniversalParser[+CT <: CFMMType, F[_]: Monad](implicit
    orders: CFMMOrdersParser[CT, F],
    pools: CFMMPoolsParser[CT],
    evals: CFMMOrderEvaluationParser[F]
  ) extends CFMMHistoryParser[CT, F] {

    def swap(tx: Transaction): F[Option[EvaluatedCFMMOrder[Swap, SwapEvaluation]]] =
      parseSomeOrder(tx)(orders.swap, (o, _, a) => evals.parseSwapEval(o, a))
        .mapIn(x => x.copy(order = x.order.copy(timestamp = tx.timestamp)))

    def deposit(tx: Transaction): F[Option[EvaluatedCFMMOrder[Deposit, DepositEvaluation]]] =
      parseSomeOrder(tx)(orders.deposit, evals.parseDepositEval)
        .mapIn(x => x.copy(order = x.order.copy(timestamp = tx.timestamp)))

    def redeem(tx: Transaction): F[Option[EvaluatedCFMMOrder[Redeem, RedeemEvaluation]]] =
      parseSomeOrder(tx)(orders.redeem, evals.parseRedeemEval)
        .mapIn(x => x.copy(order = x.order.copy(timestamp = tx.timestamp)))

    private def parseSomeOrder[A <: CFMMOrder, E <: OrderEvaluation](
      tx: Transaction
    )(
      opParser: Output => F[Option[A]],
      evalParse: (Output, CFMMPool, A) => F[Option[E]]
    ): F[Option[EvaluatedCFMMOrder[A, E]]] =
      tx.inputs
        .map(_.asOutput)
        .traverse { i =>
          opParser(i)
            .flatMap {
              case Some(ord) =>
                val pool = pools.pool(i)
                tx.outputs
                  .traverse(o => pools.pool(i).fold(none[E].pure[F])(p => evalParse(o, p.confirmed, ord)))
                  .map(_.headOption.flatten)
                  .map(eval => (ord.some, eval, pool))
              case None => (none[A], none[E], none[Confirmed[CFMMPool]]).pure[F]
            }
        }
        .map {
          _.collectFirst { case (Some(order), maybeEval, maybePool) =>
            EvaluatedCFMMOrder(order, maybeEval, maybePool.map(_.confirmed))
          }
        }

  }
}
