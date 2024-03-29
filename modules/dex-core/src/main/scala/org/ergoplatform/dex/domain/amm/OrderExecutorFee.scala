package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ErgoAddress
import org.ergoplatform.ergo.{BoxId, PubKey}
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class OrderExecutorFee(
  poolId: PoolId,
  orderId: OrderId,
  outputId: BoxId,
  address: String,
  operatorFee: Long,
  timestamp: Long
)
