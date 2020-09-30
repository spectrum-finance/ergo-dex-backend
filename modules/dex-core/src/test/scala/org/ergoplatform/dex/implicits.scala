package org.ergoplatform.dex

import org.ergoplatform.ErgoAddressEncoder

object implicits {

  implicit def ergoAddressEncoder: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)
}
