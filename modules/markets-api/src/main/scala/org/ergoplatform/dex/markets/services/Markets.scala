package org.ergoplatform.dex.markets.services

import org.ergoplatform.dex.domain.Market
import org.ergoplatform.ergo.TokenId

trait Markets[F[_]] {

  /** Get all available markets.
    */
  def getAll: F[Set[Market]]

  /** Get markets involving an asset with the given id.
    */
  def getByAsset(id: TokenId): F[List[Market]]
}
