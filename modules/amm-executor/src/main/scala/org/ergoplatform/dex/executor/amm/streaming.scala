package org.ergoplatform.dex.executor.amm

import org.ergoplatform.dex.domain.amm.{CFMMOperationRequest, OperationId}
import org.ergoplatform.common.streaming.Consumer

object streaming {

  type CFMMConsumer[F[_], G[_]] = Consumer[OperationId, CFMMOperationRequest, F, G]
}
