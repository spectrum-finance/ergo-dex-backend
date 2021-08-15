package org.ergoplatform.ergo.explorer

import org.ergoplatform.ergo.BoxId
import org.scalatest.matchers.should
import org.scalatest.propspec.AnyPropSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class TxSubmissionErrorParserSpec extends AnyPropSpec with should.Matchers with ScalaCheckPropertyChecks {

  val text =
    """Bad request: Transaction is invalid. Input 0:ca25edacddd58816910c714219da920e9814b46a6dfe4f4b683d5c32e2353c64 not found; Input 1:35d62bd9c17773007552a04e269b889551ed2891ce7c2238f54a93133ef4b14b not found"""

  val inputs = List(
    (BoxId.fromStringUnsafe("ca25edacddd58816910c714219da920e9814b46a6dfe4f4b683d5c32e2353c64"), 0),
    (BoxId.fromStringUnsafe("35d62bd9c17773007552a04e269b889551ed2891ce7c2238f54a93133ef4b14b"), 1)
  )

  property("TX error parsing") {
    val parser = TxSubmissionErrorParser.default
    parser.missedInputs(text) shouldBe inputs
  }
}
