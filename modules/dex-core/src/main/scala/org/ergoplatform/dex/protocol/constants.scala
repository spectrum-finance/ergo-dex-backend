package org.ergoplatform.dex.protocol

import org.ergoplatform.dex.{HexString, TokenId}

object constants {

  val PreGenesisHeight = 0

  val NativeAssetId: TokenId =
    TokenId(HexString.fromBytes(Array.fill(32)(0: Byte)))
}
