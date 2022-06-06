package org.ergoplatform.ergo.services.explorer

import org.ergoplatform.common.HexString
import org.ergoplatform.ergo.TokenId
import sttp.model.Uri.Segment

object paths {

  val checkTransactionPathSeg: Segment     = Segment("api/v1/mempool/transactions/submit", identity)
  val blocksPathSeg: Segment               = Segment("api/v1/blocks", identity)
  val blocksStreamPathSeg: Segment         = Segment("api/v1/blocks/byGlobalIndex/stream", identity)
  val blockSummariesStreamPathSeg: Segment = Segment("api/v1/blocks/stream/summary", identity)
  val infoPathSeg: Segment                 = Segment("api/v1/info", identity)
  val paramsPathSeg: Segment               = Segment("api/v1/epochs/params", identity)
  val utxoPathSeg: Segment                 = Segment("api/v1/boxes/unspent/byGlobalIndex/stream", identity)
  val txoPathSeg: Segment                  = Segment("api/v1/boxes/byGlobalIndex/stream", identity)
  val txPathSeg: Segment                   = Segment("api/v1/transactions/byGlobalIndex/stream", identity)

  def txsByScriptsPathSeg(templateHash: HexString): Segment =
    Segment(s"api/v1/transactions/byInputsScriptTemplateHash/$templateHash", identity)

  def utxoByTokenIdPathSeg(tokenId: TokenId): Segment =
    Segment(s"api/v1/boxes/unspent/byTokenId/$tokenId", identity)

  def tokenInfoPathSeg(tokenId: TokenId): Segment =
    Segment(s"api/v1/tokens/$tokenId", identity)
}
