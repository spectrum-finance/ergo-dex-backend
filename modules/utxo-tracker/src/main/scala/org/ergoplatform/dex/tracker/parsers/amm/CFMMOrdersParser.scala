package org.ergoplatform.dex.tracker.parsers.amm

import cats.Monad
import cats.effect.Clock
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.ergo.models.Output
import tofu.higherKind.Embed

trait CFMMOrdersParser[+CT <: CFMMType, F[_]] {

  def deposit(box: Output): F[Option[Deposit]]

  def redeem(box: Output): F[Option[Redeem]]

  def swap(box: Output): F[Option[Swap]]
}

object CFMMOrdersParser {

  def apply[CT <: CFMMType, F[_]](implicit ev: CFMMOrdersParser[CT, F]): CFMMOrdersParser[CT, F] = ev

  implicit def embed[CT <: CFMMType]: Embed[CFMMOrdersParser[CT, *[_]]] = {
    type Rep[F[_]] = CFMMOrdersParser[CT, F]
    tofu.higherKind.derived.genEmbed[Rep]
  }

  implicit def t2tCfmmOps[F[_]: Monad: Clock](implicit
    e: ErgoAddressEncoder
  ): CFMMOrdersParser[T2T_CFMM, F] =
    T2TCFMMOrdersParser.make[F]

  implicit def n2tCfmmOps[F[_]: Monad: Clock](implicit
    e: ErgoAddressEncoder
  ): CFMMOrdersParser[N2T_CFMM, F] =
    N2TCFMMOrdersParser.make[F]
}
