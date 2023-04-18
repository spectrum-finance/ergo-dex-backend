package org.ergoplatform.dex.executor.amm.config

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.ergo.{Address, TokenId}
import sigmastate.basics.DLogProtocol.DLogProverInput
import tofu.Context
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class ExchangeConfig(spectrumToken: TokenId, mnemonic: String)

object ExchangeConfig extends Context.Companion[ExchangeConfig]
