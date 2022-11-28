package org.ergoplatform.dex.protocol.amm

sealed trait InterpreterVersion

object InterpreterVersion {
  sealed trait V1 extends InterpreterVersion

  sealed trait V2 extends InterpreterVersion

  sealed trait V3 extends InterpreterVersion

  type Any = InterpreterVersion
}
