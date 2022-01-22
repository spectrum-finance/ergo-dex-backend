package org.ergoplatform.dex.protocol

sealed trait ProtoVer

object ProtoVer {
  case object V0 extends ProtoVer
  type V0 = V0.type

  case object V1 extends ProtoVer
  type V1 = V1.type

  case object V2 extends ProtoVer
  type V2 = V2.type
}
