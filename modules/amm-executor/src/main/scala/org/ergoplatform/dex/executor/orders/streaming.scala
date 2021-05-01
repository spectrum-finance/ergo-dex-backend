package org.ergoplatform.dex.executor.orders

import org.ergoplatform.dex.domain.amm.{CfmmOperation, OperationId}
import org.ergoplatform.dex.streaming.Consumer

object streaming {

  type CfmmConsumer[F[_], G[_]] = Consumer[OperationId, CfmmOperation, F, G]
}