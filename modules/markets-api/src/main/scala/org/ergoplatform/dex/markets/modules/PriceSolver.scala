package org.ergoplatform.dex.markets.modules

import org.ergoplatform.dex.domain.{AssetEquiv, FullAsset, Market, ValueUnits}
import org.ergoplatform.dex.markets.services.Markets
import org.ergoplatform.dex.protocol.constants.{NativeAssetId, NativeAssetTicker}
import org.ergoplatform.ergo.TokenId

trait PriceSolver[F[_]] {

  def solve(asset: FullAsset, targetUnits: ValueUnits): F[AssetEquiv]
}

object PriceSolver {

  class Live[F[_]] extends PriceSolver[F] {

    sealed trait Path

    case class Link(elem: Market, next: Path) extends Path

    case object Stub extends Path

    def algo(markets: Set[Market], assetId: TokenId, path: Path) = {
      val primaryAssetPairs = markets.filter(m => m.x.tokenId == assetId || m.y.tokenId == assetId).map( x =>
        if (x.y.tokenId == assetId) x.x
        else x.y
      )
      val isErgoPairExistsForEach = primaryAssetPairs.map{c =>
        markets.find(x => (x.x.tokenId == c.tokenId || x.y.tokenId == c.tokenId)
        && (x.x.tokenId == NativeAssetId || x.y.tokenId == NativeAssetId))}

      if (isErgoPairExistsForEach.exists(_.isDefined)) {
        val ergPair = isErgoPairExistsForEach.find(_.isDefined).flatten.get
        val asset = if(ergPair.x.tokenId == NativeAssetId) ergPair.y else ergPair.x
        ( ??? , ergPair)
      }
      else primaryAssetPairs.map(x => algo(markets, x.tokenId, Path()))
    }

    override def solve(asset: FullAsset, targetUnits: ValueUnits): F[AssetEquiv] =
      ???
  }

}