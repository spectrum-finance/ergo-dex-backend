package org.ergoplatform.dex.clients.explorer

import org.ergoplatform.dex.{HexString, TokenId}
import sttp.model.Uri.Segment

object paths {

  val submitTransactionPathSeg: Segment = Segment("api/v0/transactions/send", identity)
  val blocksPathSeg: Segment            = Segment("api/v0/blocks", identity)
  val paramsPathSeg: Segment            = Segment("api/v1/epochs/params", identity)
  val utxoPathSeg: Segment              = Segment("api/v1/boxes/unspent/byLastEpochs", identity)

  def txsByScriptsPathSeg(templateHash: HexString): Segment =
    Segment(s"api/v1/transactions/byInputsScriptTemplateHash/$templateHash", identity)

  def utxoByTokenIdPathSeg(tokenId: TokenId): Segment =
    Segment(s"api/v1/boxes/unspent/byTokenId/$tokenId", identity)
}
