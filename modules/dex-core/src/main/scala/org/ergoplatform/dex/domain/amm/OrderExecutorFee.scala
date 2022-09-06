package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.{BoxId, PubKey}
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class OrderExecutorFee(poolId: Option[PoolId], orderId: OrderId, outputId: BoxId, address: PubKey, operatorFee: Long, timestamp: Long)
