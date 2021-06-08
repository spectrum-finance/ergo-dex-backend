package org.ergoplatform.dex.tracker

import org.ergoplatform.network.models.Output

package object handlers {
  type BoxHandler[F[_]] = F[Output] => F[Unit]
}
