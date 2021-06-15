package org.ergoplatform.dex.resolver

import org.ergoplatform.ergo.models.Output

package object handlers {
  type BoxHandler[F[_]] = F[Output] => F[Unit]
}
