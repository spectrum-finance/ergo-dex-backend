package org.ergoplatform.dex.tracker.parsers.amm

import cats.Monad
import cats.effect.Clock
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.ergo.models.Output
import tofu.higherKind.Embed

trait AMMOpsParser[+CT <: CFMMType, F[_]] {

  def deposit(box: Output): F[Option[Deposit]]

  def redeem(box: Output): F[Option[Redeem]]

  def swap(box: Output): F[Option[Swap]]
}

object AMMOpsParser {

  def apply[CT <: CFMMType, F[_]](implicit ev: AMMOpsParser[CT, F]): AMMOpsParser[CT, F] = ev

  implicit def embed[CT <: CFMMType]: Embed[AMMOpsParser[CT, *[_]]] = {
    type Rep[F[_]] = AMMOpsParser[CT, F]
    tofu.higherKind.derived.genEmbed[Rep]
  }

  implicit def t2tCfmmOps[F[_]: Monad: Clock](implicit
    e: ErgoAddressEncoder
  ): AMMOpsParser[T2T_CFMM, F] =
    T2TCFMMOpsParser.make[F]

  implicit def n2tCfmmOps[F[_]: Monad: Clock](implicit
    e: ErgoAddressEncoder
  ): AMMOpsParser[N2T_CFMM, F] =
    N2TCFMMOpsParser.make[F]
}
