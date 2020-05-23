package org.ergoplatform.dex.domain.models

import org.ergoplatform.dex.{Address, BoxId}

final case class OrderMeta(
  boxId: BoxId,
  ownerAddress: Address,
  ts: Long
)
