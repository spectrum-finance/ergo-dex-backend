package org.ergoplatform.dex.markets.models.amm

import org.ergoplatform.dex.markets.domain.{TotalValueLocked, Volume}

final case class PlatformSummary(tvl: TotalValueLocked, volume: Volume)
