package org.ergoplatform.network

object errors {

  final case class ResponseError(msg: String) extends Exception(msg)
}
