package org.ergoplatform.ergo.state

import derevo.cats.show
import derevo.circe.{decoder, encoder}
import derevo.derive
import scodec.Codec
import sttp.tapir.{Schema, Validator}
import tofu.logging.derivation.loggable

/** Unconfirmed (mempool) entity state.
  */
@derive(loggable, encoder, decoder, show)
final case class Unconfirmed[T](entity: T)

object Unconfirmed {
  implicit def schema[T: Schema]: Schema[Unconfirmed[T]]          = Schema.derived
  implicit def validator[T: Validator]: Validator[Unconfirmed[T]] = Validator.pass

  implicit def codec[T: Codec]: Codec[Unconfirmed[T]] = implicitly[Codec[T]].as[Unconfirmed[T]]

  implicit def ledgerStatus: LedgerStatus[Unconfirmed] =
    new LedgerStatus[Unconfirmed] {
      def lift[A](a: A): Unconfirmed[A] = Unconfirmed(a)
    }
}
