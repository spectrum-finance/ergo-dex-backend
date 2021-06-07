package org.ergoplatform.dex.tracker

import org.ergoplatform.dex.network.Output

package object handlers {
  type BoxHandler[F[_]] = F[Output] => F[Unit]
}
