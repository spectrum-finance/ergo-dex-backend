package org.ergoplatform.dex.domain

import tofu.Errors

object errors {

  final case class TxFailed(reason: String) extends Exception(s"TX failed. $reason")

  object TxFailed extends Errors.Companion[TxFailed]
}
