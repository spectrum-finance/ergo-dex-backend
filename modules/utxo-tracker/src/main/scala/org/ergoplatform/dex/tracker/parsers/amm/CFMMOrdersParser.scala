package org.ergoplatform.dex.tracker.parsers.amm

import cats.Monad
import cats.effect.Clock
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.amm.CFMMOrder
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.dex.protocol.amm.ParserVersion
import org.ergoplatform.dex.tracker.parsers.amm.v1.{N2TOrdersV1Parser, T2TOrdersV1Parser}
import org.ergoplatform.dex.tracker.parsers.amm.v2.{N2TOrdersV2Parser, T2TOrdersV2Parser}
import org.ergoplatform.dex.tracker.parsers.amm.v3.{N2TOrdersV3Parser, T2TOrdersV3Parser}
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.ergo.domain.Output
import tofu.higherKind.Embed

trait CFMMOrdersParser[+CT <: CFMMType, +T <: ParserVersion, F[_]] {

  def deposit(box: Output): F[Option[CFMMOrder.AnyDeposit]]

  def redeem(box: Output): F[Option[CFMMOrder.AnyRedeem]]

  def swap(box: Output): F[Option[CFMMOrder.AnySwap]]
}

object CFMMOrdersParser {

  def apply[CT <: CFMMType, F[_], T <: ParserVersion](implicit
    ev: CFMMOrdersParser[CT, T, F]
  ): CFMMOrdersParser[CT, T, F] = ev

  implicit def embed[CT <: CFMMType, T <: ParserVersion]: Embed[CFMMOrdersParser[CT, T, *[_]]] = {
    type Rep[F[_]] = CFMMOrdersParser[CT, T, F]
    tofu.higherKind.derived.genEmbed[Rep]
  }

  implicit def v1T2TParser[F[_]: Monad: Clock](implicit
    e: ErgoAddressEncoder
  ): CFMMOrdersParser[T2T_CFMM, ParserVersion.V1, F] =
    T2TOrdersV1Parser.make[F]

  implicit def v1N2TParser[F[_]: Monad: Clock](implicit
    e: ErgoAddressEncoder
  ): CFMMOrdersParser[N2T_CFMM, ParserVersion.V1, F] =
    N2TOrdersV1Parser.make[F]

  implicit def v2T2TParser[F[_]: Monad: Clock](implicit
    e: ErgoAddressEncoder
  ): CFMMOrdersParser[T2T_CFMM, ParserVersion.V2, F] =
    T2TOrdersV2Parser.make[F]

  implicit def v2N2TParser[F[_]: Monad: Clock](implicit
    e: ErgoAddressEncoder
  ): CFMMOrdersParser[N2T_CFMM, ParserVersion.V2, F] =
    N2TOrdersV2Parser.make[F]

  implicit def v3T2TParser[F[_]: Monad: Clock](implicit
    spf: TokenId,
    e: ErgoAddressEncoder
  ): CFMMOrdersParser[T2T_CFMM, ParserVersion.V3, F] =
    T2TOrdersV3Parser.make[F](spf)

  implicit def v3N2TParser[F[_]: Monad: Clock](implicit
    spf: TokenId,
    e: ErgoAddressEncoder
  ): CFMMOrdersParser[N2T_CFMM, ParserVersion.V3, F] =
    N2TOrdersV3Parser.make[F](spf)

}
