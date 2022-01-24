package org.ergoplatform.common

trait IsOption[A] {
  def none: A
}

object IsOption {

  implicit def optionIsOption[A]: IsOption[Option[A]] =
    new IsOption[Option[A]] {
      def none: Option[A] = None
    }
}
