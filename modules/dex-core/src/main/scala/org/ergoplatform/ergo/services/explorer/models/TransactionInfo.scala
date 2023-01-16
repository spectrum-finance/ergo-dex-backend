package org.ergoplatform.ergo.services.explorer.models

import derevo.circe.decoder
import derevo.derive
import io.circe.Json
import org.ergoplatform.common.HexString
import org.ergoplatform.ergo.{Address, BlockId, BoxId, TokenId, TokenType, TxId}

@derive(decoder)
final case class TransactionInfo(
  id: TxId,
  headerId: BlockId,
  inclusionHeight: Int,
  timestamp: Long,
  index: Int,
  confirmationsCount: Int,
  inputs: List[InputInfo],
  dataInputs: List[DataInputInfo],
  outputs: List[OutputInfo]
)

@derive(decoder)
final case class InputInfo(
  id: BoxId,
  value: Option[Long],
  index: Int,
  spendingProof: Option[HexString],
  transactionId: TxId,
  outputTransactionId: Option[TxId],
  outputIndex: Option[Int],
  address: Option[Address]
)

@derive(decoder)
final case class DataInputInfo(
  id: BoxId,
  value: Option[Long],
  index: Int,
  transactionId: TxId,
  outputTransactionId: Option[TxId],
  outputIndex: Option[Int],
  address: Option[Address]
)

@derive(decoder)
final case class OutputInfo(
  id: BoxId,
  txId: TxId,
  value: Long,
  index: Int,
  creationHeight: Int,
  ergoTree: HexString,
  address: Address,
  assets: List[AssetInstanceInfo],
  additionalRegisters: Json,
  spentTransactionId: Option[TxId],
  mainChain: Boolean
)

@derive(decoder)
final case class AssetInstanceInfo(
  tokenId: TokenId,
  index: Int,
  amount: Long,
  name: Option[String],
  decimals: Option[Int],
  `type`: Option[TokenType]
)
