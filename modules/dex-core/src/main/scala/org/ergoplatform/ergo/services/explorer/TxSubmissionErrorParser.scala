package org.ergoplatform.ergo.services.explorer

import org.ergoplatform.ergo.BoxId

trait TxSubmissionErrorParser {

  /** Extract an index of a missed input.
    */
  def missedInputs(error: String): List[(BoxId, Int)]
}

object TxSubmissionErrorParser {

  implicit val default: TxSubmissionErrorParser =
    new TxSubmissionErrorParser {
      val TxErrP         = """^Bad request: Transaction is invalid. (.+)""".r
      val InputNotFoundP = """^Input ([0-9]+):([0-9a-fA-F]+) not found$""".r
      val InputSpentP    = """^Input ([0-9]+):([0-9a-fA-F]+) was spent$""".r

      def missedInputs(error: String): List[(BoxId, Int)] =
        error match {
          case TxErrP(errs) =>
            errs.split("; ").toList.collect {
              case InputNotFoundP(i, idHex) =>
                BoxId.fromStringUnsafe(idHex) -> i.toInt
              case InputSpentP(i, idHex) =>
                BoxId.fromStringUnsafe(idHex) -> i.toInt
            }
          case _ => List.empty
        }
    }
}
