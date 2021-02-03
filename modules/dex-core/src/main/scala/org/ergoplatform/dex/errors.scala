package org.ergoplatform.dex

object errors {

  final case class RefinementFailed(details: String) extends Exception(s"Refinement failed: $details")
}
