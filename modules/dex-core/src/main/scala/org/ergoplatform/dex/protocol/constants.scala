package org.ergoplatform.dex.protocol

import org.ergoplatform.ergo.BoxId
import org.ergoplatform.common.HexString
import org.ergoplatform.dex.domain.{AssetClass, AssetInfo, CryptoUnits, Ticker}
import org.ergoplatform.ergo.TokenId

object constants {

  val PreGenesisHeight = 0

  val ErgoAssetId: TokenId =
    TokenId(HexString.fromBytes(Array.fill(32)(0: Byte)))

  val ErgoGenesisBox: BoxId =
    BoxId(ErgoAssetId.unwrapped)

  val ErgoEmissionAmount: Long = 93409065000000000L

  val ErgoAssetTicker: Ticker = Ticker("ERG")

  val ErgoAssetDescription: String = "Ergo Blockchain native token"

  val ErgoAssetDecimals = 9

  val ErgoAssetClass: AssetClass = AssetClass(ErgoAssetId, Some(ErgoAssetTicker), Some(ErgoAssetDecimals))

  val ErgoUnits: CryptoUnits = CryptoUnits(ErgoAssetClass)
}
