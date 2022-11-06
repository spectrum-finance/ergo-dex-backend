package org.ergoplatform.dex.tracker.handlers

import cats.effect.Clock
import cats.implicits.none
import cats.instances.list._
import cats.syntax.traverse._
import cats.{Functor, FunctorFilter, Monad}
import mouse.any._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.common.streaming.{Producer, Record}
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.dex.protocol.amm.ParserType
import org.ergoplatform.dex.tracker.parsers.amm.CFMMOrdersParser
import org.ergoplatform.dex.tracker.validation.amm.CFMMRules
import org.ergoplatform.ergo.state.LedgerStatus
import tofu.logging.{Logging, Logs}
import tofu.streams.Evals
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

final class CFMMOpsHandler[
  F[_]: Monad: Evals[*[_], G]: FunctorFilter,
  G[_]: Monad: Logging,
  Status[_]: LedgerStatus
](parsers: List[CFMMOrdersParser[CFMMType, ParserType.Any, G]])(implicit
  producer: Producer[OrderId, Status[CFMMOrder[CFMMOrderType.Any]], F],
  rules: CFMMRules[G]
) {

  def handler: BoxHandler[F] =
    _.evalMap { out =>
      parsers
        .traverse { parser =>
          for {
            deposit <- parser.deposit(out)
            redeem  <- parser.redeem(out)
            swap    <- parser.swap(out)
          } yield deposit orElse redeem orElse swap orElse none[CFMMOrder[CFMMOrderType.Any]]
        }
        .map(_.reduce(_ orElse _))
    }.unNone
      .evalTap(op => info"CFMM operation request detected $op")
      .flatMap { op =>
        eval(rules(op)) >>=
          (_.fold(op.pure[F])(e => eval(debug"Rule violation: $e") >> Evals[F, G].monoidK.empty))
      }
      .map(op => Record[OrderId, Status[CFMMOrder[CFMMOrderType.Any]]](op.id, LedgerStatus.lift(op)))
      .thrush(producer.produce)
}

object CFMMOpsHandler {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: FunctorFilter,
    G[_]: Monad: Clock,
    Status[_]: LedgerStatus
  ](implicit
    producer: Producer[OrderId, Status[CFMMOrder.Any], F],
    rules: CFMMRules[G],
    logs: Logs[I, G],
    encoder: ErgoAddressEncoder
  ): I[BoxHandler[F]] =
    logs.forService[CFMMOpsHandler[F, G, Status]].map { implicit log =>
      val parsers =
        CFMMOrdersParser[T2T_CFMM, G, ParserType.Default] ::
        CFMMOrdersParser[N2T_CFMM, G, ParserType.Default] ::
        CFMMOrdersParser[T2T_CFMM, G, ParserType.MultiAddress] ::
        CFMMOrdersParser[N2T_CFMM, G, ParserType.MultiAddress] ::
        Nil
      new CFMMOpsHandler[F, G, Status](parsers).handler
    }
}
