package org.ergoplatform.dex.protocol.amm.parsers

import org.ergoplatform.dex.domain.amm.{CfmmPool, PoolId}
import org.ergoplatform.dex.domain.{AssetAmount, BoxInfo, OnChain}
import org.ergoplatform.dex.protocol.amm.AmmContractType.T2tCfmm
import org.ergoplatform.dex.protocol.amm.constants
import org.ergoplatform.ergo.models.SConstant.IntConstant
import org.ergoplatform.ergo.models.{Output, RegisterId}

object T2tCfmmPoolParser extends CfmmPoolParser[T2tCfmm] {

  def parse(box: Output): Option[OnChain[CfmmPool]] =
    for {
      nft <- box.assets.lift(constants.cfmm.t2t.IndexNFT)
      lp  <- box.assets.lift(constants.cfmm.t2t.IndexLP)
      x   <- box.assets.lift(constants.cfmm.t2t.IndexX)
      y   <- box.assets.lift(constants.cfmm.t2t.IndexY)
      fee <- box.additionalRegisters.get(RegisterId.R4).collect { case IntConstant(x) => x }
    } yield OnChain(
      CfmmPool(
        PoolId(nft.tokenId),
        AssetAmount.fromBoxAsset(lp),
        AssetAmount.fromBoxAsset(x),
        AssetAmount.fromBoxAsset(y),
        fee,
        BoxInfo.fromBox(box)
      )
    )
}
