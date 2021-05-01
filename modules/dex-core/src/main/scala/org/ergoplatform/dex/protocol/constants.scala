package org.ergoplatform.dex.protocol

import org.ergoplatform.dex.{TokenId, HexString}

object constants {

  val PreGenesisHeight = 0

  val NativeAssetId: TokenId =
    TokenId(HexString.unsafeFromString("b0244dfc267baca974a4caee06120321562784303a8a688976ae56170e4d175b"))
}
