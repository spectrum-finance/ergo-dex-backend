package org.ergoplatform.dex.tracker.parsers.amm

import cats.Monad
import cats.data.OptionT
import org.ergoplatform.dex.domain.amm.{CFMMOrderType, CFMMOrderVersion, CFMMVersionedOrder, EvaluatedCFMMOrder, OrderEvaluation}
import org.ergoplatform.dex.domain.amm.CFMMVersionedOrder._
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.ergo.domain.Output
import tofu.syntax.monadic._

trait VersionedCFMMParser[+CT <: CFMMType, F[_]] {
  def deposit(box: Output): F[Option[CFMMVersionedOrder.AnyDeposit]]

  def redeem(box: Output): F[Option[CFMMVersionedOrder.AnyRedeem]]

  def swap(box: Output): F[Option[CFMMVersionedOrder.AnySwap]]
}

object VersionedCFMMParser {

  implicit def makeT2TCFMMVersionedParser[F[_]: Monad](implicit
    current: CFMMOrdersParser[T2T_CFMM, F],
    v0: V0Parser[T2T_CFMM, F]
  ): VersionedCFMMParser[T2T_CFMM, F] = make[T2T_CFMM, F]

  implicit def makeN2TCFMMVersionedParser[F[_]: Monad](implicit
    current: CFMMOrdersParser[N2T_CFMM, F],
    v0: V0Parser[N2T_CFMM, F]
  ): VersionedCFMMParser[N2T_CFMM, F] = make[N2T_CFMM, F]

  private def make[CT <: CFMMType, F[_]: Monad](implicit
    current: CFMMOrdersParser[CT, F],
    v0: V0Parser[CT, F]
  ): VersionedCFMMParser[CT, F] =
    new VersionedCFMMParser[CT, F] {

      def deposit(box: Output): F[Option[CFMMVersionedOrder.AnyDeposit]] =
        OptionT(current.deposit(box))
          .map(s => DepositV1(s.poolId, s.maxMinerFee, s.timestamp, s.params, s.box): CFMMVersionedOrder.AnyDeposit)
          .orElseF(v0.depositV0(box).map(r => r: Option[CFMMVersionedOrder.AnyDeposit]))
          .value

      def redeem(box: Output): F[Option[CFMMVersionedOrder.AnyRedeem]] =
        OptionT(current.redeem(box))
          .map(s => RedeemV1(s.poolId, s.maxMinerFee, s.timestamp, s.params, s.box): CFMMVersionedOrder.AnyRedeem)
          .orElseF(v0.redeemV0(box).map(r => r: Option[CFMMVersionedOrder.AnyRedeem]))
          .value

      def swap(box: Output): F[Option[CFMMVersionedOrder.AnySwap]] =
        OptionT(current.swap(box))
          .map(s => SwapV1(s.poolId, s.maxMinerFee, s.timestamp, s.params, s.box): CFMMVersionedOrder.AnySwap)
          .orElseF(v0.swapV0(box).map(r => r: Option[CFMMVersionedOrder.AnySwap]))
          .value
    }
}
