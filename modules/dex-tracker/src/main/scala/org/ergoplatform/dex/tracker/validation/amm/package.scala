package org.ergoplatform.dex.tracker.validation

import cats.{FlatMap, Monad}
import org.ergoplatform.dex.domain.NetworkContext
import org.ergoplatform.dex.domain.amm.CfmmOperation
import org.ergoplatform.dex.tracker.configs.Fees
import org.ergoplatform.network.ErgoNetwork
import tofu.higherKind.Embed
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._

package object amm {

  type CfmmRules[F[_]] = CfmmOperation => F[Boolean]

  implicit def embed: Embed[CfmmRules] =
    new Embed[CfmmRules] {

      def embed[F[_]: FlatMap](ft: F[CfmmRules[F]]): CfmmRules[F] =
        op => ft >>= (rules => rules(op))
    }

  object CfmmRules {

    def make[F[_]: Monad: Fees.Has](implicit network: ErgoNetwork[F]): CfmmRules[F] =
      (
        for {
          conf       <- context
          networkCtx <- NetworkContext.make
        } yield new CfmmRuleDefs[F](conf, networkCtx).rules
      ).embed
  }
}
