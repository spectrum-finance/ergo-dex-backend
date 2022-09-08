package org.ergoplatform.dex.markets.modules

import cats.{FlatMap, Functor, Monad}
import cats.data.OptionT
import org.ergoplatform.dex.domain._
import org.ergoplatform.dex.markets.db.models.amm.PoolSnapshot
import org.ergoplatform.dex.markets.services.{FiatRates, Markets}
import org.ergoplatform.dex.protocol.constants
import org.ergoplatform.dex.protocol.constants.ErgoAssetDecimals
import tofu.higherKind.{Mid, RepresentableK}
import tofu.logging.{Logging, Logs}
import tofu.syntax.monadic._
import tofu.syntax.foption._
import tofu.syntax.logging._

import scala.math.BigDecimal.RoundingMode

sealed trait PriceSolverType { type AssetRepr }
trait CryptoSolverType extends PriceSolverType { type AssetRepr = AssetClass }
trait FiatSolverType extends PriceSolverType { type AssetRepr = Currency }

trait PriceSolver[F[_], T <: PriceSolverType] {

  type AssetRepr = T#AssetRepr

  def convert(asset: FullAsset, target: ValueUnits[AssetRepr], pools: List[PoolSnapshot]): F[Option[AssetEquiv[AssetRepr]]]
}

object PriceSolver {

  private def parsePools(pools: List[PoolSnapshot]): List[Market] =
    pools.map(p => Market.fromReserves(p.lockedX, p.lockedY))

  type CryptoPriceSolver[F[_]] = PriceSolver[F, CryptoSolverType]

  object CryptoPriceSolver {

    implicit def representableK: RepresentableK[CryptoPriceSolver] =
      tofu.higherKind.derived.genRepresentableK

    def make[I[_]: Functor, F[_]: Monad](implicit markets: Markets[F], logs: Logs[I, F]): I[CryptoPriceSolver[F]] =
      logs
        .forService[CryptoPriceSolver[F]]
        .map(implicit l =>
          Mid.attach[CryptoPriceSolver, F](new PriceSolverTracing[F, CryptoSolverType])(new CryptoSolver(markets))
        )
  }

  type FiatPriceSolver[F[_]] = PriceSolver[F, FiatSolverType]

  object FiatPriceSolver {

    implicit def representableK: RepresentableK[FiatPriceSolver] =
      tofu.higherKind.derived.genRepresentableK

    def make[I[_]: Functor, F[_]: Monad](implicit
      rates: FiatRates[F],
      cryptoSolver: CryptoPriceSolver[F],
      logs: Logs[I, F]
    ): I[FiatPriceSolver[F]] =
      logs
        .forService[FiatPriceSolver[F]]
        .map(implicit l =>
          Mid.attach[FiatPriceSolver, F](new PriceSolverTracing[F, FiatSolverType])(
            new ViaErgFiatSolver(rates, cryptoSolver)
          )
        )
  }

  final class ViaErgFiatSolver[F[_]: Monad](rates: FiatRates[F], cryptoSolver: CryptoPriceSolver[F])
    extends PriceSolver[F, FiatSolverType] {

    def convert(asset: FullAsset, target: ValueUnits[AssetRepr], pools: List[PoolSnapshot]): F[Option[AssetEquiv[AssetRepr]]] =
      target match {
        case fiat @ FiatUnits(_) =>
          (for {
            ergEquiv <- OptionT(cryptoSolver.convert(asset, constants.ErgoUnits, pools))
            ergRate  <- OptionT(rates.rateOf(constants.ErgoAssetClass, fiat))
            fiatEquiv    = ergEquiv.value / math.pow(10, ErgoAssetDecimals - fiat.currency.decimals) * ergRate
            fiatEquivFmt = fiatEquiv.setScale(0, RoundingMode.FLOOR)
          } yield AssetEquiv(asset, fiat, fiatEquivFmt)).value
      }
  }

  final class CryptoSolver[F[_]: Monad](markets: Markets[F]) extends PriceSolver[F, CryptoSolverType] {

    def convert(asset: FullAsset, target: ValueUnits[AssetRepr], pools: List[PoolSnapshot]): F[Option[AssetEquiv[AssetRepr]]] =
      target match {
        case CryptoUnits(units) =>
          if (asset.id != units.tokenId) {
            val filtered = pools.filter { p => p.lockedX.id == asset.id || p.lockedY.id == asset.id }
            val markets1 = parsePools(filtered)
            markets1.find(_.contains(units.tokenId)).map { market =>
              val amountEquiv = BigDecimal(asset.amount) * market.priceBy(asset.id)
              AssetEquiv(asset, target, amountEquiv)
            }.pure
          } else AssetEquiv(asset, target, BigDecimal(asset.amount)).someF
      }
  }

  final class PriceSolverTracing[F[_]: FlatMap: Logging, T <: PriceSolverType] extends PriceSolver[Mid[F, *], T] {

    def convert(asset: FullAsset, target: ValueUnits[AssetRepr], pools: List[PoolSnapshot]): Mid[F, Option[AssetEquiv[AssetRepr]]] =
      for {
        _ <- trace"convert(asset=$asset, target=$target)"
        r <- _
        _ <- trace"convert(asset=$asset, target=$target) -> $r"
      } yield r
  }
}
