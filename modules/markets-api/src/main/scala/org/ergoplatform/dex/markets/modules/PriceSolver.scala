package org.ergoplatform.dex.markets.modules

import cats.Monad
import cats.data.OptionT
import org.ergoplatform.dex.domain._
import org.ergoplatform.dex.markets.services.{FiatRates, Markets}
import org.ergoplatform.dex.protocol.constants
import tofu.syntax.monadic._
import tofu.syntax.foption._

sealed trait PriceSolverType { type AssetRepr }
trait CryptoSolverType extends PriceSolverType { type AssetRepr = AssetClass }
trait FiatSolverType extends PriceSolverType { type AssetRepr = Currency }

trait PriceSolver[F[_], T <: PriceSolverType] {

  type AssetRepr = T#AssetRepr

  def solve(asset: FullAsset, target: ValueUnits[AssetRepr]): F[Option[AssetEquiv[AssetRepr]]]
}

object PriceSolver {

  final class ViaErgFiatSolver[F[_]: Monad](rates: FiatRates[F], cryptoSolver: CryptoSolver[F])
    extends PriceSolver[F, FiatSolverType] {

    def solve(asset: FullAsset, target: ValueUnits[AssetRepr]): F[Option[AssetEquiv[AssetRepr]]] =
      target match {
        case fiat @ FiatUnits(_) =>
          (for {
            ergEquiv <- OptionT(cryptoSolver.solve(asset, constants.NativeUnits))
            ergRate  <- OptionT(rates.rateOf(constants.NativeAssetClass, fiat))
            fiatEquiv = ergEquiv.value * ergRate / math.pow(10, fiat.currency.decimals.toDouble)
          } yield AssetEquiv(asset, fiat, fiatEquiv)).value
      }
  }

  final class CryptoSolver[F[_]: Monad](markets: Markets[F]) extends PriceSolver[F, CryptoSolverType] {

    def solve(asset: FullAsset, target: ValueUnits[AssetRepr]): F[Option[AssetEquiv[AssetRepr]]] =
      target match {
        case CryptoUnits(units) =>
          if (asset.id != units.tokenId) {
            markets
              .getByAsset(asset.id)
              .map(_.find(_.contains(units.tokenId)).map { market =>
                val amountEquiv = asset.amount * market.priceBy(asset.id)
                AssetEquiv(asset, target, amountEquiv)
              })
          } else AssetEquiv(asset, target, BigDecimal(asset.amount)).someF
      }
  }
}
