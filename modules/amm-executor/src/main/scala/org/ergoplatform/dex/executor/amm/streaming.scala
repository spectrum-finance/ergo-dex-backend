package org.ergoplatform.dex.executor.amm

import org.ergoplatform.dex.OperationId
import org.ergoplatform.dex.domain.amm.CfmmOperation
import org.ergoplatform.dex.streaming.Consumer

object streaming {

  type CfmmConsumer[F[_], G[_]] = Consumer[OperationId, CfmmOperation, F, G]
}
