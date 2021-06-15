package org.ergoplatform.dex.resolver

import org.ergoplatform.dex.domain.amm.CFMMPool
import org.ergoplatform.dex.domain.amm.state.{OnChain, Resolved}

trait CFMMPoolStateResolver[F[_]] {

  /** Decide which variant of CFMM pool state is most up-to-date.
    */
  def resolve(update: OnChain[CFMMPool]): F[Resolved[CFMMPool]]
}
