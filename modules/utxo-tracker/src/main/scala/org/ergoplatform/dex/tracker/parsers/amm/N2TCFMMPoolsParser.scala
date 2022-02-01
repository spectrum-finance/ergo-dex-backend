package org.ergoplatform.dex.tracker.parsers.amm

import org.ergoplatform.ergo.state.Confirmed
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.domain.{AssetAmount, AssetInfo, BoxInfo}
import org.ergoplatform.dex.protocol.amm.AMMType.N2T_CFMM
import org.ergoplatform.dex.protocol.amm.constants
import org.ergoplatform.ergo.domain.SConstant.IntConstant
import org.ergoplatform.ergo.domain.{Output, RegisterId}

object N2TCFMMPoolsParser extends CFMMPoolsParser[N2T_CFMM] {

  def pool(box: Output): Option[Confirmed[CFMMPool]] =
    for {
      nft <- box.assets.lift(constants.cfmm.n2t.IndexNFT)
      lp  <- box.assets.lift(constants.cfmm.n2t.IndexLP)
      y   <- box.assets.lift(constants.cfmm.n2t.IndexY)
      fee <- box.additionalRegisters.get(RegisterId.R4).collect { case IntConstant(x) => x }
    } yield Confirmed(
      CFMMPool(
        PoolId(nft.tokenId),
        AssetAmount.fromBoxAsset(lp),
        AssetAmount.native(box.value),
        AssetAmount.fromBoxAsset(y),
        AssetInfo(lp),
        AssetInfo.native,
        AssetInfo(y),
        fee,
        BoxInfo.fromBox(box)
      )
    )
}
