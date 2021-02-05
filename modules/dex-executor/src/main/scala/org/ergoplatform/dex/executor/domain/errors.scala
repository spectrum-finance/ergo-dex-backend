package org.ergoplatform.dex.executor.domain

object errors {

  abstract class ExecutionFailure(msg: String) extends Exception(msg)

  final case class ExhaustedOutputValue(available: Long, required: Long, nanoErgsPerByte: Long)
    extends ExecutionFailure(
      s"Output value exhausted. [Available: $available, Required: $required, NanoErgsPerByte: $nanoErgsPerByte]"
    )
}
