package org.ergoplatform.dex.markets.services

import cats.effect.concurrent.Ref
import cats.effect.{Clock, Sync, Timer}
import cats.{FlatMap, Monad}
import derevo.derive
import org.ergoplatform.dex.domain.{AssetClass, FiatUnits}
import org.ergoplatform.dex.markets.currencies.UsdUnits
import org.ergoplatform.dex.protocol.constants.ErgoAssetClass
import org.ergoplatform.ergo.TokenId
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.foption._
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait FiatRates[F[_]] {

  def rateOf(asset: AssetClass, units: FiatUnits): F[Option[BigDecimal]]
}

object FiatRates {

  val ErgUsdPoolNft: TokenId =
    TokenId.fromStringUnsafe("011d3364de07e5a26f0c4eef0852cddb387039a921b7154ef3cab22c6eda887f")

  def make[I[_]: FlatMap, F[_]: Monad: Clock: Timer](implicit
    network: ErgoExplorer[F],
    logs: Logs[I, F],
    cache: Ref[F, Option[BigDecimal]]
  ): I[FiatRates[F]] =
    logs.forService[FiatRates[F]].map(implicit __ => new Tracing[F] attach new ErgoOraclesRateSource[F](network, cache))

  final class ErgoOraclesRateSource[F[_]: Monad: Logging: Timer](
    network: ErgoExplorer[F],
    cache: Ref[F, Option[BigDecimal]]
  ) extends FiatRates[F] {

    def rateOf(asset: AssetClass, units: FiatUnits): F[Option[BigDecimal]] =
      if (asset == ErgoAssetClass && units == UsdUnits) cache.get
      else noneF[F, BigDecimal]
  }

  final class Tracing[F[_]: Logging: Monad] extends FiatRates[Mid[F, *]] {

    def rateOf(asset: AssetClass, units: FiatUnits): Mid[F, Option[BigDecimal]] =
      for {
        _ <- trace"Memo read $asset $units"
        r <- _
        _ <- trace"Memo read completed $r"
      } yield r
  }
}
