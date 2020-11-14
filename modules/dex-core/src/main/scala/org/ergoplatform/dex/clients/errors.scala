package org.ergoplatform.dex.clients

object errors {

  final case class ResponseError(msg: String) extends Exception(msg)
}
