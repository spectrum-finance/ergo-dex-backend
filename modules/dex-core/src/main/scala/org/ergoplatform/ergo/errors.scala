package org.ergoplatform.ergo

object errors {

  final case class ResponseError(msg: String) extends Exception(msg)
}
