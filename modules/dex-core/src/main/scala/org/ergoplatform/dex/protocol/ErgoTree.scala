package org.ergoplatform.dex.protocol

import org.ergoplatform.dex.HexString

trait ErgoTree[F[_]] {

  def parse(raw: HexString): F[sigmastate.Values.ErgoTree]
}
