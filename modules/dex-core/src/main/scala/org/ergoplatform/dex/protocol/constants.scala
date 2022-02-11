package org.ergoplatform.dex.protocol

import org.ergoplatform.common.HexString
import org.ergoplatform.dex.domain.{AssetClass, AssetInfo, CryptoUnits, Ticker}
import org.ergoplatform.ergo.TokenId

object constants {

  val PreGenesisHeight = 0

  val ErgoAssetId: TokenId =
    TokenId(HexString.fromBytes(Array.fill(32)(0: Byte)))

  val ErgoAssetTicker: Ticker = Ticker("ERG")

  val ErgoAssetDecimals = 9

  val ErgoAssetInfo: AssetInfo = AssetInfo(Some(ErgoAssetTicker), Some(ErgoAssetDecimals))

  val ErgoAssetClass: AssetClass = AssetClass(ErgoAssetId, Some(ErgoAssetTicker), Some(ErgoAssetDecimals))

  val ErgoUnits: CryptoUnits = CryptoUnits(ErgoAssetClass)
}
