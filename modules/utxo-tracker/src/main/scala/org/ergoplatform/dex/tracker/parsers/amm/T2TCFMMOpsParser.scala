package org.ergoplatform.dex.tracker.parsers.amm

import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.T2TCFMM
import org.ergoplatform.dex.protocol.amm.ContractTemplates
import org.ergoplatform.ergo.models.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{Address, ErgoTreeTemplate, TokenId}
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}

final class T2TCFMMOpsParser(implicit
  templates: ContractTemplates[T2TCFMM],
  e: ErgoAddressEncoder
) extends AMMOpsParser[T2TCFMM] {

  def deposit(box: Output): Option[Deposit] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == templates.deposit) {
      for {
        poolId <- tree.constants.parseBytea(8).map(PoolId.fromBytes)
        inX    <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount, a.name))
        inY    <- box.assets.lift(1).map(a => AssetAmount(a.tokenId, a.amount, a.name))
        dexFee <- tree.constants.parseLong(10)
        p2pk   <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
        params = DepositParams(inX, inY, dexFee, p2pk)
      } yield Deposit(poolId, params, box)
    } else None
  }

  def redeem(box: Output): Option[Redeem] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == templates.redeem) {
      for {
        poolId <- tree.constants.parseBytea(10).map(PoolId.fromBytes)
        inLP   <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount, a.name))
        dexFee <- tree.constants.parseLong(12)
        p2pk   <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
        params = RedeemParams(inLP, dexFee, p2pk)
      } yield Redeem(poolId, params, box)
    } else None
  }

  def swap(box: Output): Option[Swap] = {
    val tree     = ErgoTreeSerializer.default.deserialize(box.ergoTree)
    val template = ErgoTreeTemplate.fromBytes(tree.template)
    if (template == templates.swap) {
      for {
        poolId       <- tree.constants.parseBytea(9).map(PoolId.fromBytes)
        inAmount     <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount, a.name))
        outId        <- tree.constants.parseBytea(3).map(TokenId.fromBytes)
        minOutAmount <- tree.constants.parseLong(12)
        out = AssetAmount(outId, minOutAmount, None)
        dexFeePerTokenNum   <- tree.constants.parseLong(13)
        dexFeePerTokenDenom <- tree.constants.parseLong(14)
        p2pk                <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
        params = SwapParams(inAmount, out, dexFeePerTokenNum, dexFeePerTokenDenom, p2pk)
      } yield Swap(poolId, params, box)
    } else None
  }
}
