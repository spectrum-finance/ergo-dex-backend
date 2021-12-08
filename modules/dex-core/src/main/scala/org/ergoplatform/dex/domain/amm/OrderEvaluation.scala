package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.dex.domain.AssetAmount
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
sealed trait OrderEvaluation

object OrderEvaluation {
  @derive(encoder, decoder, loggable)
  final case class SwapEvaluation(output: AssetAmount) extends OrderEvaluation
  @derive(encoder, decoder, loggable)
  final case class DepositEvaluation(outputLP: AssetAmount) extends OrderEvaluation
  @derive(encoder, decoder, loggable)
  final case class RedeemEvaluation(outputX: AssetAmount, outputY: AssetAmount) extends OrderEvaluation
}
