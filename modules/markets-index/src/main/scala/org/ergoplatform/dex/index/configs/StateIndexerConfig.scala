package org.ergoplatform.dex.index.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.domain.amm.PoolId
import org.ergoplatform.ergo.TokenId
import pureconfig.ConfigReader

@derive(pureconfigReader)
final case class StateIndexerConfig(pools: Map[TokenId, PoolId])

object StateIndexerConfig {

  implicit val configReaderMap: ConfigReader[Map[TokenId, PoolId]] =
    ConfigReader.mapReader[PoolId].map(elems => elems.map { case (k, v) => TokenId.fromStringUnsafe(k) -> v })
}
