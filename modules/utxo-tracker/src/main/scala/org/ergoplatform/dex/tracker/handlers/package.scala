package org.ergoplatform.dex.tracker

import org.ergoplatform.ergo.domain.{Output, SettledOutput, Transaction}

package object handlers {
  type BoxHandler[F[_]]        = F[Output] => F[Unit]
  type SettledBoxHandler[F[_]] = F[SettledOutput] => F[Unit]
  type TxHandler[F[_]]         = F[Transaction] => F[Unit]
}
