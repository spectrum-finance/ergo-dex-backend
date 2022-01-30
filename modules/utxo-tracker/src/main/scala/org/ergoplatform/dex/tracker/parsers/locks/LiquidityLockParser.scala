package org.ergoplatform.dex.tracker.parsers.locks

import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm.state.Confirmed
import org.ergoplatform.dex.domain.locks.LiquidityLock
import org.ergoplatform.dex.domain.locks.types.LockId
import org.ergoplatform.dex.protocol.{ErgoTreeSerializer, ProtoVer}
import org.ergoplatform.dex.protocol.locks.LiquidityLockTemplates
import org.ergoplatform.ergo.{Address, ErgoTreeTemplate}
import org.ergoplatform.ergo.models.SConstant.{IntConstant, SigmaPropConstant}
import org.ergoplatform.ergo.models.{Output, RegisterId}
import sigmastate.basics.DLogProtocol.ProveDlog
import sigmastate.serialization.GroupElementSerializer

import scala.util.Try

trait LiquidityLockParser[V <: ProtoVer] {
  def parse(out: Output): Option[Confirmed[LiquidityLock]]
}

object LiquidityLockParser {

  implicit def locksV0(implicit e: ErgoAddressEncoder): LiquidityLockParser[ProtoVer.V0] = new ImplV0

  final class ImplV0(implicit templates: LiquidityLockTemplates[ProtoVer.V0], e: ErgoAddressEncoder)
    extends LiquidityLockParser[ProtoVer.V0] {

    def parse(out: Output): Option[Confirmed[LiquidityLock]] = {
      val tree     = ErgoTreeSerializer.default.deserialize(out.ergoTree)
      val template = ErgoTreeTemplate.fromBytes(tree.template)
      if (template == templates.lock) {
        for {
          deadline      <- out.additionalRegisters.get(RegisterId.R4).collect { case IntConstant(d) => d }
          redeemer      <- out.additionalRegisters.get(RegisterId.R5).collect { case SigmaPropConstant(pk) => pk }
          amount        <- out.assets.headOption.map(AssetAmount.fromBoxAsset)
          redeemerPoint <- Try(GroupElementSerializer.fromBytes(redeemer.toBytes)).toOption
          redeemerAddress = Address.fromStringUnsafe(e.toString(P2PKAddress(ProveDlog(redeemerPoint))))
        } yield Confirmed(LiquidityLock(LockId.fromBoxId(out.boxId), deadline, amount, redeemerAddress))
      } else None
    }
  }
}
