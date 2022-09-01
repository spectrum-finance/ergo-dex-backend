package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ErgoAddress
import org.ergoplatform.ergo.{Address, BoxId}
import org.ergoplatform.ergo.domain.Output
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class OffChainOperator(orderId: OrderId, outputId: BoxId, address: String, operatorFee: Long)
