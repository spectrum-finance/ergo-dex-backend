package org.ergoplatform.ergo.node

import sttp.model.Uri.Segment

object paths {
  val submitTransactionPathSeg: Segment = Segment("transactions", identity)
}
