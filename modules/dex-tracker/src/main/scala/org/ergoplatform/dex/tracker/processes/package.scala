package org.ergoplatform.dex.tracker

import org.ergoplatform.dex.domain.network.Output

package object processes {
  type BoxHandler[F[_]] = F[Output] => F[Unit]
}
