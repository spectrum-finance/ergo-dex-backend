package org.ergoplatform.common.http.cache

import io.estatico.newtype.macros.newtype
import scodec.Codec
import scodec.bits.ByteVector
import scorex.crypto.hash.{Blake2b256, Digest32}
import _root_.scodec.interop.cats._
import _root_.scodec.codecs._
import cats.Show
import tofu.logging.Loggable

object types {

  @newtype
  case class Hash32(value: ByteVector)

  object Hash32 {
    implicit val show: Show[Hash32]         = deriving
    implicit val loggable: Loggable[Hash32] = Loggable.show
    implicit val codec: Codec[Hash32]       = fixedSizeBytes(32, bytes).xmap(Hash32(_), _.value)

    def apply(bytes: Seq[Byte]*): Hash32 = Hash32(ByteVector(Blake2b256.hash(bytes.flatten.toArray) !@@ Digest32))

  }
}
