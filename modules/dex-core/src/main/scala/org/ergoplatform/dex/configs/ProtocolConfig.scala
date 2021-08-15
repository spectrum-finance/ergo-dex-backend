package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.protocol.Network
import tofu.Context
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class ProtocolConfig(networkType: Network)

object ProtocolConfig extends Context.Companion[ProtocolConfig]
