package org.ergoplatform.dex.protocol

import org.ergoplatform.dex.HexString
import scorex.util.encode.Base16
import sigmastate.Values
import sigmastate.serialization.{ErgoTreeSerializer => TreeSerializeer}
import tofu.ApplicativeThrow

trait ErgoTreeSerializer[F[_]] {

  def serialize(tree: sigmastate.Values.ErgoTree): HexString

  def deserialize(raw: HexString): F[sigmastate.Values.ErgoTree]
}

object ErgoTreeSerializer {

  implicit def instance[F[_]](implicit F: ApplicativeThrow[F]): ErgoTreeSerializer[F] =
    new ErgoTreeSerializer[F] {

      def serialize(tree: Values.ErgoTree): HexString =
        HexString.fromBytes(TreeSerializeer.DefaultSerializer.serializeErgoTree(tree))

      def deserialize(raw: HexString): F[Values.ErgoTree] =
        F.fromTry(Base16.decode(raw.unwrapped).map(TreeSerializeer.DefaultSerializer.deserializeErgoTree))
    }
}
