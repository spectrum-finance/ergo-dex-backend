package org.ergoplatform.dex.domain

import org.ergoplatform.ergo.TokenId

final case class AssetClass(tokenId: TokenId, ticker: Option[Ticker], decimals: Option[Int])
