package org.ergoplatform.ergo.modules

import derevo.derive
import org.ergoplatform.ergo.domain.Output
import tofu.higherKind.derived.representableK

@derive(representableK)
trait MempoolStreaming[F[_]] {

  def streamUnspentOutputs: F[Output]
}
