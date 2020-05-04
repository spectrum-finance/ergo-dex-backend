package org.ergoplatform.dex

import scala.util.control.NoStackTrace

trait Err extends Exception with NoStackTrace {
  def msg: String
  override def getMessage: String = msg
}

object Err {

  final case class RefinementFailed(details: String) extends Err {
    val msg: String = s"Refinement failed: $details"
  }
}
