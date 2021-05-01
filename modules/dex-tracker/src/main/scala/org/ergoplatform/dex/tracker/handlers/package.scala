package org.ergoplatform.dex.tracker

import org.ergoplatform.dex.domain.network.Output

package object handlers {
  type BoxHandler[F[_]] = F[Output] => F[Unit]
}
