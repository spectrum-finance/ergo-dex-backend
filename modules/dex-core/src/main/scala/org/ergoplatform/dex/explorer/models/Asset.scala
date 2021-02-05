package org.ergoplatform.dex.explorer.models

import derevo.circe.decoder
import derevo.derive
import org.ergoplatform.dex.AssetId
import tofu.logging.derivation.loggable

/** A model mirroring Asset entity from Ergo node REST API.
  * See `Asset` in https://github.com/ergoplatform/ergo/blob/master/src/main/resources/api/openapi.yaml
  */
@derive(decoder, loggable)
final case class Asset(
  tokenId: AssetId,
  amount: Long
)
