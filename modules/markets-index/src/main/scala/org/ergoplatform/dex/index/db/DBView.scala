package org.ergoplatform.dex.index.db

trait DBView[A, B] {
  def dbView(a: A): B
}

object DBView {

  object syntax {

    implicit class DBViewOps[A](protected val a: A) extends AnyVal {
      def dbView[B](implicit ev: DBView[A, B]): B = ev.dbView(a)
    }
  }
}
