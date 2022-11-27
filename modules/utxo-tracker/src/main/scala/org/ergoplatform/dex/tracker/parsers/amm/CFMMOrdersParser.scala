package org.ergoplatform.dex.tracker.parsers.amm

import cats.Monad
import cats.effect.Clock
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.domain.amm.CFMMOrder._
import org.ergoplatform.dex.domain.amm.CFMMOrderType.FeeType.ErgFee
import org.ergoplatform.dex.protocol.amm.AMMType.{CFMMType, N2T_CFMM, T2T_CFMM}
import org.ergoplatform.dex.protocol.amm.ParserType
import org.ergoplatform.ergo.PubKey
import org.ergoplatform.ergo.domain.Output
import tofu.higherKind.Embed

trait CFMMOrdersParser[+CT <: CFMMType, +T <: ParserType, F[_]] {

  def deposit(box: Output): F[Option[Deposit[ErgFee, PubKey]]]

  def redeem(box: Output): F[Option[Redeem[ErgFee, PubKey]]]

  def swap(box: Output): F[Option[SwapErgAny]]
}

object CFMMOrdersParser {

  def apply[CT <: CFMMType, F[_], T <: ParserType](implicit
    ev: CFMMOrdersParser[CT, T, F]
  ): CFMMOrdersParser[CT, T, F] = ev

  implicit def embed[CT <: CFMMType, T <: ParserType]: Embed[CFMMOrdersParser[CT, T, *[_]]] = {
    type Rep[F[_]] = CFMMOrdersParser[CT, T, F]
    tofu.higherKind.derived.genEmbed[Rep]
  }

  implicit def t2tCfmmOps[F[_]: Monad: Clock](implicit
    e: ErgoAddressEncoder
  ): CFMMOrdersParser[T2T_CFMM, ParserType.Default, F] =
    T2TCFMMOrdersParserP2Pk.make[F]

  implicit def n2tCfmmOps[F[_]: Monad: Clock](implicit
    e: ErgoAddressEncoder
  ): CFMMOrdersParser[N2T_CFMM, ParserType.Default, F] =
    N2TCFMMOrdersParserP2Pk.make[F]

  implicit def t2tCfmmOpsMultiAddr[F[_]: Monad: Clock](implicit
    e: ErgoAddressEncoder
  ): CFMMOrdersParser[T2T_CFMM, ParserType.MultiAddress, F] =
    T2TCFMMOrdersParserMultiAddress.make[F]

  implicit def n2tCfmmOpsMultiAddr[F[_]: Monad: Clock](implicit
    e: ErgoAddressEncoder
  ): CFMMOrdersParser[N2T_CFMM, ParserType.MultiAddress, F] =
    N2TCFMMOrdersParserMultiAddress.make[F]

}
