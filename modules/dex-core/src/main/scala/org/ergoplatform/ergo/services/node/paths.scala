package org.ergoplatform.ergo.services.node

import sttp.model.Uri.Segment

object paths {
  val submitTransactionPathSeg: Segment = Segment("transactions", identity)
  val unconfirmedTransactionsPathSeg: Segment = Segment("transactions/unconfirmed", identity)
}
