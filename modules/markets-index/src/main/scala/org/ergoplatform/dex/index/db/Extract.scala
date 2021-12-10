package org.ergoplatform.dex.index.db

trait Extract[A, B] {
  def extract(a: A): B
}

object Extract {

  object syntax {

    implicit class ExtractOps[A](protected val a: A) extends AnyVal {
      def extract[B](implicit ev: Extract[A, B]): B = ev.extract(a)
    }
  }
}
