package org.ergoplatform.dex
import org.ergoplatform.dex.streaming.CommitPolicy
import tofu.HasContext

package object context {

  type HasCommitPolicy[F[_]] = F HasContext CommitPolicy
}
