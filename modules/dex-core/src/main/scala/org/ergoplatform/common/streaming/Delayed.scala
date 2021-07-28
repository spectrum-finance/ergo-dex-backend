package org.ergoplatform.common.streaming

final case class Delayed[A](message: A, blockedUntil: Long)
