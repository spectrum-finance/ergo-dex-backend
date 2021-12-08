package org.ergoplatform.dex.tracker

import org.ergoplatform.ergo.models.{Output, Transaction}

package object handlers {
  type BoxHandler[F[_]] = F[Output] => F[Unit]
  type TxHandler[F[_]]  = F[Transaction] => F[Unit]
}
