package org.ergoplatform.dex.executor.amm.modules

import org.ergoplatform.dex.domain.amm.CFMMOrder

trait ExecutionBacklog[F[_]] {

  def put(order: CFMMOrder): F[Unit]

  def pop: F[Option[CFMMOrder]]
}
