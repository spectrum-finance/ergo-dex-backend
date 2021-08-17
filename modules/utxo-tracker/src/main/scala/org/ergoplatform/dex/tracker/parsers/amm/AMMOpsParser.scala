package org.ergoplatform.dex.tracker.parsers.amm

import cats.Functor
import cats.effect.Clock
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, T2T_CFMM}
import org.ergoplatform.dex.protocol.amm.ContractTemplates
import org.ergoplatform.ergo.models.Output

trait AMMOpsParser[CT <: CFMMType, F[_]] {

  def deposit(box: Output): F[Option[Deposit]]

  def redeem(box: Output): F[Option[Redeem]]

  def swap(box: Output): F[Option[Swap]]
}

object AMMOpsParser {

  implicit def t2tCfmmOps[F[_]: Functor: Clock](implicit
                                                ts: ContractTemplates[T2T_CFMM],
                                                e: ErgoAddressEncoder
  ): AMMOpsParser[T2T_CFMM, F] =
    new T2TCFMMOpsParser()
}
