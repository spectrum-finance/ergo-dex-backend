package org.ergoplatform.ergo.services.explorer

trait TxSubmissionErrorParser {

  /** Extract an index of a missed input.
    */
  def missedInputs(error: String): List[Int]
}

object TxSubmissionErrorParser {

  val InvalidPoolIndex = 0
  val InvalidDexOutputIndex = 2

  implicit val default: TxSubmissionErrorParser =
    new TxSubmissionErrorParser {

      def missedInputs(error: String): List[Int] = {
        val requiredPrefix = "Missing inputs"
        val split          = error.split(":").toList
        if (split.dropRight(1).lastOption.exists(_.contains(requiredPrefix))) {
          split.lastOption
            .map(_.replace(" ", "").toList.map(_.getNumericValue))
            .getOrElse(List.empty[Int])
        } else List.empty[Int]
      }
    }
}
