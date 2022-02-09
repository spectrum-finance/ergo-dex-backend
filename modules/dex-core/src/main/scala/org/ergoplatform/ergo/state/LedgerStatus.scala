package org.ergoplatform.ergo.state

/** Type-class allowing to add to an on-chain entity [A] type level information
  * about it's on-chain status (Confirmed|Unconfirmed|Predicted).
  */
trait LedgerStatus[C[_]] {
  def lift[A](a: A): C[A]
}

object LedgerStatus {
  def lift[C[_], A](a: A)(implicit ev: LedgerStatus[C]): C[A] = ev.lift(a)
}
