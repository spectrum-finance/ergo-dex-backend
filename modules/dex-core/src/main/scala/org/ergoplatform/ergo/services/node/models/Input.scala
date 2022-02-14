package org.ergoplatform.ergo.services.node.models

import derevo.circe.{decoder, encoder}
import derevo.derive
import org.ergoplatform.ergo.BoxId
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class Input(boxId: BoxId)
