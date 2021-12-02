package org.ergoplatform.dex.index.repos

import cats.data.NonEmptyList
import derevo.derive
import org.ergoplatform.dex.domain.amm.CFMMPool
import tofu.higherKind.derived.representableK

@derive(representableK)
trait PoolsRepo[F[_]] {

  def insert(pools: NonEmptyList[CFMMPool]): F[Int]
}
