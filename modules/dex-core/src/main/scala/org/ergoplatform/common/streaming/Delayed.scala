package org.ergoplatform.common.streaming

import derevo.circe.{decoder, encoder}
import derevo.derive
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
final case class Delayed[A](message: A, blockedUntil: Long)
