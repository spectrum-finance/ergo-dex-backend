package org.ergoplatform.dex.resolver.parsers.amm

import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.domain.amm._
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.AMMType.T2TCFMM
import org.ergoplatform.dex.protocol.amm.ContractTemplates
import org.ergoplatform.ergo.models.Output
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.ergo.{Address, ErgoTreeTemplate, TokenId}
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}

final class T2TCfmmOpsParser(implicit
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
        poolId    <- tree.constants.parseBytea(8).map(PoolId.fromBytes)
        in        <- box.assets.lift(0).map(a => AssetAmount(a.tokenId, a.amount, a.name))
        outId     <- tree.constants.parseBytea(3).map(TokenId.fromBytes)
        outAmount <- tree.constants.parseLong(11)
        out = AssetAmount(outId, outAmount, None)
        dexFeePerToken <- tree.constants.parseLong(12)
        p2pk           <- tree.constants.parsePk(0).map(pk => Address.fromStringUnsafe(P2PKAddress(pk).toString))
        params = SwapParams(in, out, dexFeePerToken, p2pk)
      } yield Swap(poolId, params, box)
    } else None
  }
}
