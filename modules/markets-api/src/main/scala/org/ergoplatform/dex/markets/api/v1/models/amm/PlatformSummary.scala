package org.ergoplatform.dex.markets.api.v1.models.amm

import org.ergoplatform.dex.markets.domain.{TotalValueLocked, Volume}

final case class PlatformSummary(tvl: TotalValueLocked, volume: Volume)
