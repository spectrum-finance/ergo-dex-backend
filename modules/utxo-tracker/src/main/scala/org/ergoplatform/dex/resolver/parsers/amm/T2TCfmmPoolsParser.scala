package org.ergoplatform.dex.resolver.parsers.amm

import org.ergoplatform.dex.domain.amm.state.OnChain
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.domain.{AssetAmount, BoxInfo}
import org.ergoplatform.dex.protocol.amm.AMMType.T2TCFMM
import org.ergoplatform.dex.protocol.amm.constants
import org.ergoplatform.ergo.models.SConstant.IntConstant
import org.ergoplatform.ergo.models.{Output, RegisterId}

object T2TCfmmPoolsParser extends CFMMPoolsParser[T2TCFMM] {

  def pool(box: Output): Option[OnChain[CFMMPool]] =
    for {
      nft <- box.assets.lift(constants.cfmm.t2t.IndexNFT)
      lp  <- box.assets.lift(constants.cfmm.t2t.IndexLP)
      x   <- box.assets.lift(constants.cfmm.t2t.IndexX)
      y   <- box.assets.lift(constants.cfmm.t2t.IndexY)
      fee <- box.additionalRegisters.get(RegisterId.R4).collect { case IntConstant(x) => x }
    } yield OnChain(
      CFMMPool(
        PoolId(nft.tokenId),
        AssetAmount.fromBoxAsset(lp),
        AssetAmount.fromBoxAsset(x),
        AssetAmount.fromBoxAsset(y),
        fee,
        BoxInfo.fromBox(box)
      )
    )
}
