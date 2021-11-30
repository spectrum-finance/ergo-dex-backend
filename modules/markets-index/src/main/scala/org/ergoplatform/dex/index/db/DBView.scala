package org.ergoplatform.dex.index.db

trait DBView[A, B] {
  def viewDB(a: A): B
}

object DBView {

  object syntax {

    implicit class DBViewOps[A](protected val a: A) extends AnyVal {
      def viewDB[B](implicit ev: DBView[A, B]): B = ev.viewDB(a)
    }
  }
}
