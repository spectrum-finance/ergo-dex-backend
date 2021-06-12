package org.ergoplatform.dex.executor.amm.services

import org.ergoplatform.dex.domain.amm.CfmmOperation
import org.ergoplatform.dex.executor.amm.repositories.CfmmPools

trait Execution[F[_]] {

  def execute(op: CfmmOperation): F[Unit]
}

object Execution {

  final class Live[F[_]](pools: CfmmPools[F]) extends Execution[F] {

    def execute(op: CfmmOperation): F[Unit] = ???
  }
}
