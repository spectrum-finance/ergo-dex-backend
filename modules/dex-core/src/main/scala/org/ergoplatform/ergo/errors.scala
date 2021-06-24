package org.ergoplatform.ergo

import tofu.Errors

object errors {

  final case class ResponseError(msg: String) extends Exception(msg)

  object ResponseError extends Errors.Companion[ResponseError]
}
