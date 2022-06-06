package org.ergoplatform.ergo.services.explorer.models

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.common.HexString
import org.ergoplatform.ergo.BlockId

@derive(decoder)
final case class HeaderInfo(
  id: BlockId,
  parentId: BlockId,
  version: Short,
  height: Int,
  epoch: Int,
  difficulty: BigDecimal,
  adProofsRoot: HexString,
  stateRoot: HexString,
  transactionsRoot: HexString,
  timestamp: Long,
  nBits: Long,
  size: Int,
  extensionHash: HexString,
  powSolutions: PowSolutionInfo,
  votes: (Byte, Byte, Byte)
)

@derive(decoder)
final case class PowSolutionInfo(pk: HexString, w: HexString, n: HexString, d: String)
