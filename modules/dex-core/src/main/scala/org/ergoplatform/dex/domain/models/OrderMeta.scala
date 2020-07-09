package org.ergoplatform.dex.domain.models

import org.ergoplatform.P2PKAddress
import org.ergoplatform.dex.BoxId

final case class OrderMeta(
  boxId: BoxId,
  boxValue: Long,
  ownerAddress: P2PKAddress,
  ts: Long
)
