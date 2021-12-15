package org.ergoplatform.dex.protocol

import org.ergoplatform.common.HexString
import org.ergoplatform.dex.domain.{AssetClass, CryptoUnits, Ticker}
import org.ergoplatform.ergo.TokenId

object constants {

  val PreGenesisHeight = 0

  val NativeAssetId: TokenId =
    TokenId(HexString.fromBytes(Array.fill(32)(0: Byte)))

  val NativeAssetTicker: Ticker = Ticker("ERG")

  val NativeAssetDecimals = 9

  val NativeAssetClass: AssetClass = AssetClass(NativeAssetId, Some(NativeAssetTicker), Some(NativeAssetDecimals))

  val NativeUnits: CryptoUnits = CryptoUnits(NativeAssetClass)
}
