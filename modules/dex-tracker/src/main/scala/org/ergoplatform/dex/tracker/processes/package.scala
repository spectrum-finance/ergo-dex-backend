package org.ergoplatform.dex.tracker

import org.ergoplatform.dex.clients.explorer.models.Output

package object processes {
  type BoxHandler[F[_]] = F[Output] => F[Unit]
}
