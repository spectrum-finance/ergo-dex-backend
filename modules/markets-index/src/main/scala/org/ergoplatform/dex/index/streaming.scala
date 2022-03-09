package org.ergoplatform.dex.index

import fs2.kafka.types.KafkaOffset
import org.ergoplatform.common.streaming.Consumer
import org.ergoplatform.ergo.state.{Confirmed, ConfirmedIndexed}
import org.ergoplatform.dex.domain.amm.{CFMMPool, EvaluatedCFMMOrder, OrderId, PoolId}
import org.ergoplatform.dex.domain.locks.LiquidityLock
import org.ergoplatform.dex.domain.locks.types.LockId
import org.ergoplatform.ergo.BlockId
import org.ergoplatform.ergo.domain.Block

object streaming {
  type CFMMHistConsumer[F[_], G[_]]  = Consumer.Aux[OrderId, Option[EvaluatedCFMMOrder.Any], KafkaOffset, F, G]
  type CFMMPoolsConsumer[F[_], G[_]] = Consumer.Aux[PoolId, ConfirmedIndexed[CFMMPool], KafkaOffset, F, G]
  type LqLocksConsumer[F[_], G[_]]   = Consumer.Aux[LockId, Confirmed[LiquidityLock], KafkaOffset, F, G]
  type BlocksConsumer[F[_], G[_]]   = Consumer.Aux[BlockId, Block, KafkaOffset, F, G]
}
