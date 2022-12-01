package org.ergoplatform.dex.tracker.parsers.amm.analytics

import cats.Monad
import cats.data.OptionT
import org.ergoplatform.dex.domain.amm.CFMMOrder._
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
    v0: LegacyContractsParser[T2T_CFMM, F],
    v1: CFMMOrdersParser[T2T_CFMM, ParserVersion.V1, F],
    v2: CFMMOrdersParser[T2T_CFMM, ParserVersion.V2, F],
    v3: CFMMOrdersParser[T2T_CFMM, ParserVersion.V3, F]
  ): CFMMParser[T2T_CFMM, F] = make[T2T_CFMM, F]

  implicit def makeN2TCFMMVersionedParser[F[_]: Monad](implicit
    v0: LegacyContractsParser[N2T_CFMM, F],
    v1: CFMMOrdersParser[N2T_CFMM, ParserVersion.V1, F],
    v2: CFMMOrdersParser[N2T_CFMM, ParserVersion.V2, F],
    v3: CFMMOrdersParser[N2T_CFMM, ParserVersion.V3, F]
  ): CFMMParser[N2T_CFMM, F] = make[N2T_CFMM, F]

  private def make[CT <: CFMMType, F[_]: Monad](implicit
    v0: LegacyContractsParser[CT, F],
    v1: CFMMOrdersParser[CT, ParserVersion.V1, F],
    v2: CFMMOrdersParser[CT, ParserVersion.V2, F],
    v3: CFMMOrdersParser[CT, ParserVersion.V3, F]
  ): CFMMParser[CT, F] =
    new CFMMParser[CT, F] {

      def deposit(box: Output): F[Option[CFMMVersionedOrder.AnyDeposit]] =
        OptionT(v3.deposit(box))
          .map { case DepositTokenFee(poolId, maxMinerFee, timestamp, params, box) =>
            DepositV3(poolId, maxMinerFee, timestamp, params, box): CFMMVersionedOrder.AnyDeposit
          }
          .orElseF(
            OptionT(v1.deposit(box)).map { case s: DepositErgFee =>
              DepositV2(s.poolId, s.maxMinerFee, s.timestamp, s.params, s.box): CFMMVersionedOrder.AnyDeposit
            }.value
          )
          .orElseF(v0.depositV1(box).map(r => r: Option[CFMMVersionedOrder.AnyDeposit]))
          .orElseF(v0.depositV0(box).map(r => r: Option[CFMMVersionedOrder.AnyDeposit]))
          .value

      def redeem(box: Output): F[Option[CFMMVersionedOrder.AnyRedeem]] =
        OptionT(v3.redeem(box))
          .map { case RedeemTokenFee(poolId, maxMinerFee, timestamp, params, box) =>
            RedeemV3(poolId, maxMinerFee, timestamp, params, box): CFMMVersionedOrder.AnyRedeem
          }
          .orElseF(
            OptionT(v1.redeem(box)).map { case r: RedeemErgFee =>
              RedeemV1(r.poolId, r.maxMinerFee, r.timestamp, r.params, r.box): CFMMVersionedOrder.AnyRedeem
            }.value
          )
          .orElseF(v0.redeemV0(box).map(r => r: Option[CFMMVersionedOrder.AnyRedeem]))
          .value

      def swap(box: Output): F[Option[CFMMVersionedOrder.AnySwap]] =
        OptionT(v3.swap(box))
          .map { case s: SwapTokenFee =>
            SwapV3(s.poolId, s.maxMinerFee, s.timestamp, s.params, box, s.reservedExFee): CFMMVersionedOrder.AnySwap
          }
          .orElseF(
            OptionT(v2.swap(box)).map { case s: CFMMOrder.SwapMultiAddress =>
              SwapV2(s.poolId, s.maxMinerFee, s.timestamp, s.params, s.box): CFMMVersionedOrder.AnySwap
            }.value
          )
          .orElseF(
            OptionT(v1.swap(box)).map { case s: CFMMOrder.SwapP2Pk =>
              SwapV1(s.poolId, s.maxMinerFee, s.timestamp, s.params, s.box): CFMMVersionedOrder.AnySwap
            }.value
          )
          .orElseF(v0.swapV0(box).map(r => r: Option[CFMMVersionedOrder.AnySwap]))
          .value
    }
}
