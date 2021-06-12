package org.ergoplatform.dex.tracker

import org.ergoplatform.ergo.models.Output

package object handlers {
  type BoxHandler[F[_]] = F[Output] => F[Unit]
}
