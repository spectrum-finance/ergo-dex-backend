package org.ergoplatform.dex.tracker.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context

@derive(pureconfigReader)
final case class Fees(minerFee: Long)

object Fees extends Context.Companion[Fees]
