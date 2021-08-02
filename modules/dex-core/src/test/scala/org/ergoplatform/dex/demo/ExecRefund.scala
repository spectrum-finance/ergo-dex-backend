package org.ergoplatform.dex.demo

import org.ergoplatform.ErgoBox.{NonMandatoryRegisterId, R4, R5, R6}
import org.ergoplatform.dex.demo.IssueAsset.getInput
import org.ergoplatform.dex.protocol.codecs._
import org.ergoplatform.ergo.syntax._
import org.ergoplatform.wallet.interpreter.ErgoUnsafeProver
import org.ergoplatform.{ErgoBoxCandidate, UnsignedErgoLikeTransaction, UnsignedInput}
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.SType.AnyOps
import sigmastate.Values.{Constant, EvaluatedValue}
import sigmastate.eval.Extensions._
import sigmastate.eval._
import sigmastate.{SByte, SCollection, SType}

object ExecRefund extends App with SigmaPlatform {

  val secretHex = ""
  val inputId   = "30974274078845f263b4f21787e33cc99e9ec19a17ad85a5bc6da2cca91c5a2e"

  val input = getInput(inputId).get
}
