package org.ergoplatform.ergo.domain

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import doobie.util.Write
import org.ergoplatform.ErgoBox
import org.ergoplatform.ergo.TokenId
import shapeless.Lazy
import tofu.logging.derivation.loggable
import org.ergoplatform.ergo.services.explorer.models.{BoxAsset => ExplorerBoxAsset}
import org.ergoplatform.ergo.services.node.models.{BoxAsset => NodeAsset}
import scorex.util.encode.Base16

@derive(show, encoder, decoder, loggable)
final case class BoxAsset(
  tokenId: TokenId,
  amount: Long
)

object BoxAsset {
  implicit def write: Write[BoxAsset] = Lazy(implicitly[Write[BoxAsset]]).value

  def fromErgo(id: ErgoBox.TokenId, amount: Long): BoxAsset =
    BoxAsset(TokenId.fromStringUnsafe(Base16.encode(id)), amount)

  def fromExplorer(a: ExplorerBoxAsset): BoxAsset =
    BoxAsset(a.tokenId, a.amount)

  def fromNode(a: NodeAsset): BoxAsset =
    BoxAsset(a.tokenId, a.amount)
}
