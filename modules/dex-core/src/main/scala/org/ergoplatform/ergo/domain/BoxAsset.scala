package org.ergoplatform.ergo.domain

import derevo.circe.{decoder, encoder}
import derevo.derive
import doobie.util.Write
import org.ergoplatform.ergo.TokenId
import shapeless.Lazy
import tofu.logging.derivation.loggable
import org.ergoplatform.ergo.services.explorer.models.{BoxAsset => ExplorerBoxAsset}
import org.ergoplatform.ergo.services.node.models.{BoxAsset => NodeAsset}
import scodec.Codec
import scodec.codecs.int64

@derive(encoder, decoder, loggable)
final case class BoxAsset(
  tokenId: TokenId,
  amount: Long
)

object BoxAsset {
  implicit def write: Write[BoxAsset] = Lazy(implicitly[Write[BoxAsset]]).value

  def fromExplorer(a: ExplorerBoxAsset): BoxAsset =
    BoxAsset(a.tokenId, a.amount)

  def fromNode(a: NodeAsset): BoxAsset =
    BoxAsset(a.tokenId, a.amount)

  implicit val codec: Codec[BoxAsset] =
    (implicitly[Codec[TokenId]] :: int64).as[BoxAsset]
}
