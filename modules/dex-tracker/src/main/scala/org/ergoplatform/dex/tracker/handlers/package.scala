package org.ergoplatform.dex.tracker

import org.ergoplatform.dex.clients.explorer.models.Output

package object handlers {
  type BoxHandler[F[_]] = F[Output] => F[Unit]
}
