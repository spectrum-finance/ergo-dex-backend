package org.ergoplatform.dex.executor.amm

import org.ergoplatform.dex.domain.amm.{CfmmOperation, OperationId}
import org.ergoplatform.common.streaming.Consumer

object streaming {

  type CfmmConsumer[F[_], G[_]] = Consumer[OperationId, CfmmOperation, F, G]
}
