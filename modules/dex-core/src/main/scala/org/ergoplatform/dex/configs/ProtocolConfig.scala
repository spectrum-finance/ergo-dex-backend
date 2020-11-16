package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.protocol.Network

@derive(pureconfigReader)
final case class ProtocolConfig(networkType: Network)
