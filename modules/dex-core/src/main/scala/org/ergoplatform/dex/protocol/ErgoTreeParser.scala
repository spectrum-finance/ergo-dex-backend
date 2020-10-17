package org.ergoplatform.dex.protocol

import org.ergoplatform.dex.HexString
import scorex.util.encode.Base16
import sigmastate.serialization.ErgoTreeSerializer
import tofu.ApplicativeThrow

trait ErgoTreeParser[F[_]] {

  def apply(raw: HexString): F[sigmastate.Values.ErgoTree]
}

object ErgoTreeParser {

  implicit def instance[F[_]](implicit F: ApplicativeThrow[F]): ErgoTreeParser[F] = (raw: HexString) =>
    F.fromTry(Base16.decode(raw.unwrapped).map(ErgoTreeSerializer.DefaultSerializer.deserializeErgoTree))
}
