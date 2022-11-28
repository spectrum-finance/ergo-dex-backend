package org.ergoplatform.dex.tracker.parsers.amm.analytics

import cats.Monad
import cats.effect.Clock
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.amm.CFMMVersionedOrder._
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.ergo.domain.Output
import tofu.higherKind.Embed

trait LegacyContractsParser[+CT <: CFMMType, F[_]] {

  def depositV1(box: Output): F[Option[DepositV1]]

  def depositV0(box: Output): F[Option[DepositV0]]

  def redeemV0(box: Output): F[Option[RedeemV0]]

  def swapV0(box: Output): F[Option[SwapV0]]
}

object LegacyContractsParser {

  implicit def embed[CT <: CFMMType]: Embed[LegacyContractsParser[CT, *[_]]] = {
    type Rep[F[_]] = LegacyContractsParser[CT, F]
    tofu.higherKind.derived.genEmbed[Rep]
  }

  implicit def t2tCFMMV0Parser[F[_]: Monad: Clock](implicit e: ErgoAddressEncoder): LegacyContractsParser[T2T_CFMM, F] =
    T2TCFMMOrdersLegacyContractsParser.make[F]

  implicit def n2tCFMMV0Parser[F[_]: Monad: Clock](implicit e: ErgoAddressEncoder): LegacyContractsParser[N2T_CFMM, F] =
    N2TCFMMOrdersLegacyContractsParser.make[F]
}
