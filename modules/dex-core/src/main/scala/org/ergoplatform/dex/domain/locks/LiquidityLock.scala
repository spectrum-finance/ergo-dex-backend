package org.ergoplatform.dex.domain.locks

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.locks.types.LockId
import org.ergoplatform.ergo.Address
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable, show)
final case class LiquidityLock(id: LockId, deadline: Int, amount: AssetAmount, redeemer: Address)
