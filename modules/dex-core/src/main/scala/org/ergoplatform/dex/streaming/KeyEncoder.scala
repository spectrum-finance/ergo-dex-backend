package org.ergoplatform.dex.streaming

trait KeyEncoder[-A] {

  def encode(a: A): String
}

object KeyEncoder {

  def apply[A](implicit ke: KeyEncoder[A]): KeyEncoder[A] = ke
}
