package org.ergoplatform.dex.tracker.parsers.amm.analytics

import cats.Monad
import cats.data.OptionT
import org.ergoplatform.dex.domain.amm.CFMMOrder.{DepositErgFee, RedeemErgFee}
import org.ergoplatform.dex.domain.amm.CFMMVersionedOrder._
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.dex.protocol.amm.ParserVersion
import org.ergoplatform.dex.tracker.parsers.amm.CFMMOrdersParser
import org.ergoplatform.ergo.domain.Output
import tofu.syntax.monadic._

trait CFMMParser[+CT <: CFMMType, F[_]] {
  def deposit(box: Output): F[Option[CFMMVersionedOrder.AnyDeposit]]

  def redeem(box: Output): F[Option[CFMMVersionedOrder.AnyRedeem]]

  def swap(box: Output): F[Option[CFMMVersionedOrder.AnySwap]]
}

object CFMMParser {

  implicit def makeT2TCFMMVersionedParser[F[_]: Monad](implicit
    default: CFMMOrdersParser[T2T_CFMM, ParserVersion.V1, F],
    multiAddress: CFMMOrdersParser[T2T_CFMM, ParserVersion.V2, F],
    v0: LegacyContractsParser[T2T_CFMM, F]
  ): CFMMParser[T2T_CFMM, F] = make[T2T_CFMM, F]

  implicit def makeN2TCFMMVersionedParser[F[_]: Monad](implicit
    default: CFMMOrdersParser[N2T_CFMM, ParserVersion.V1, F],
    multiAddress: CFMMOrdersParser[N2T_CFMM, ParserVersion.V2, F],
    v0: LegacyContractsParser[N2T_CFMM, F]
  ): CFMMParser[N2T_CFMM, F] = make[N2T_CFMM, F]

  private def make[CT <: CFMMType, F[_]: Monad](implicit
    v0: LegacyContractsParser[CT, F],
    v1: CFMMOrdersParser[CT, ParserVersion.V1, F],
    v2: CFMMOrdersParser[CT, ParserVersion.V2, F]
  ): CFMMParser[CT, F] =
    new CFMMParser[CT, F] {

      def deposit(box: Output): F[Option[CFMMVersionedOrder.AnyDeposit]] =
        OptionT(v1.deposit(box))
          .map { case s: DepositErgFee =>
            DepositV2(s.poolId, s.maxMinerFee, s.timestamp, s.params, s.box): CFMMVersionedOrder.AnyDeposit
          }
          .orElseF(v0.depositV1(box).map(r => r: Option[CFMMVersionedOrder.AnyDeposit]))
          .orElseF(v0.depositV0(box).map(r => r: Option[CFMMVersionedOrder.AnyDeposit]))
          .value

      def redeem(box: Output): F[Option[CFMMVersionedOrder.AnyRedeem]] =
        OptionT(v1.redeem(box))
          .map { case r: RedeemErgFee =>
            RedeemV1(r.poolId, r.maxMinerFee, r.timestamp, r.params, r.box): CFMMVersionedOrder.AnyRedeem
          }
          .orElseF(v0.redeemV0(box).map(r => r: Option[CFMMVersionedOrder.AnyRedeem]))
          .value

      def swap(box: Output): F[Option[CFMMVersionedOrder.AnySwap]] =
        OptionT(v1.swap(box))
          .orElseF(v2.swap(box))
          .map {
            case s: CFMMOrder.SwapP2Pk =>
              SwapP2Pk(s.poolId, s.maxMinerFee, s.timestamp, s.params, s.box): CFMMVersionedOrder.AnySwap
            case s: CFMMOrder.SwapMultiAddress =>
              SwapMultiAddress(s.poolId, s.maxMinerFee, s.timestamp, s.params, s.box): CFMMVersionedOrder.AnySwap
            case s: CFMMOrder.SwapTokenFee => ???
          }
          .orElseF(v0.swapV0(box).map(r => r: Option[CFMMVersionedOrder.AnySwap]))
          .value
    }
}
