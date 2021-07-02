package org.ergoplatform.common.cache

import tofu.Errors

object errors {

  final case class BinaryEncodingFailed(showValue: String, reason: String)
    extends Exception(s"Failed to binary encode value {$showValue}. $reason")

  object BinaryEncodingFailed extends Errors.Companion[BinaryEncodingFailed]

  final case class BinaryDecodingFailed(showValue: String, reason: String)
    extends Exception(s"Failed to decode value {$showValue}. $reason")

  object BinaryDecodingFailed extends Errors.Companion[BinaryDecodingFailed]
}
