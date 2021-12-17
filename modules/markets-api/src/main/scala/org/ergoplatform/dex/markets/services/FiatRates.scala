package org.ergoplatform.dex.markets.services

import cats.Monad
import org.ergoplatform.dex.domain.{AssetClass, FiatUnits}
import org.ergoplatform.dex.markets.currencies.UsdUnits
import org.ergoplatform.dex.protocol.constants.ErgoAssetClass
import org.ergoplatform.ergo.models.{RegisterId, SConstant}
import org.ergoplatform.ergo.{ErgoNetwork, TokenId}
import sigmastate.SLong
import sigmastate.Values.EvaluatedValue
import sigmastate.serialization.ValueSerializer
import tofu.syntax.foption._
import tofu.syntax.monadic._

import scala.util.Try

trait FiatRates[F[_]] {

  def rateOf(asset: AssetClass, units: FiatUnits): F[Option[BigDecimal]]
}

object FiatRates {

  val ErgUsdPoolNft = TokenId.fromStringUnsafe("011d3364de07e5a26f0c4eef0852cddb387039a921b7154ef3cab22c6eda887f")

  final class ErgoOraclesRateSource[F[_]: Monad](network: ErgoNetwork[F]) extends FiatRates[F] {

    def rateOf(asset: AssetClass, units: FiatUnits): F[Option[BigDecimal]] =
      if (asset == ErgoAssetClass && units == UsdUnits) {
        network
          .getUtxoByToken(ErgUsdPoolNft, offset = 0, limit = 1)
          .map(_.headOption)
          .map {
            for {
              out    <- _
              (_, r) <- out.additionalRegisters.find { case (r, _) => r == RegisterId.R4 }
              rawValue = r match {
                           case SConstant.ByteaConstant(raw) => Try(ValueSerializer.deserialize(raw.toBytes)).toOption
                           case _                            => None
                         }
              rate <- rawValue
                        .collect { case v: EvaluatedValue[_] => v -> v.tpe }
                        .collect { case (v, SLong) => v.value.asInstanceOf[Long] }
            } yield BigDecimal(rate) / 100
          }
      } else noneF
  }
}

