package org.ergoplatform.dex.tracker.modules.amm

import org.ergoplatform.dex.contracts.amm.cfmm.t2t
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.domain.network.Output
import org.ergoplatform.dex.domain.syntax.ergo._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AmmContractType.T2tCfmm
import org.ergoplatform.dex.{Address, ErgoTreeTemplate, TokenId}
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}

final class T2tCfmmOps(implicit e: ErgoAddressEncoder) extends AmmOps[T2tCfmm] {

  def parseDeposit(box: Output): Option[Deposit] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == t2t.depositTemplate(e.networkPrefix)) {
      for {
        poolId <- tree.constants.parseBytea(8).map(PoolId.fromBytes)
        inX    <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount, a.name))
        inY    <- box.assets.lift(1).map(a => AssetAmount(a.tokenId, a.amount, a.name))
        dexFee <- tree.constants.parseLong(10)
        p2pk   <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
        params = DepositParams(poolId, inX, inY, dexFee, p2pk)
      } yield Deposit(params, box)
    } else None
  }

  def parseRedeem(box: Output): Option[Redeem] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == t2t.depositTemplate(e.networkPrefix)) {
      for {
        poolId <- tree.constants.parseBytea(10).map(PoolId.fromBytes)
        inLP   <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount, a.name))
        dexFee <- tree.constants.parseLong(12)
        p2pk   <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
        params = RedeemParams(poolId, inLP, dexFee, p2pk)
      } yield Redeem(params, box)
    } else None
  }

  def parseSwap(box: Output): Option[Swap] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == t2t.swapTemplate(e.networkPrefix)) {
      for {
        poolId    <- tree.constants.parseBytea(8).map(PoolId.fromBytes)
        in        <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount, a.name))
        outId     <- tree.constants.parseBytea(3).map(TokenId.fromBytes)
        outAmount <- tree.constants.parseLong(11)
        out = AssetAmount(outId, outAmount, None)
        dexFeePerToken <- tree.constants.parseLong(12)
        p2pk           <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
        params = SwapParams(poolId, in, out, dexFeePerToken, p2pk)
      } yield Swap(params, box)
    } else None
  }
}
