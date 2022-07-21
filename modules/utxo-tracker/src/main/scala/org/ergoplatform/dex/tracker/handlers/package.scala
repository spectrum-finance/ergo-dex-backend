package org.ergoplatform.dex.tracker

import cats.{Functor, Monad}
import org.ergoplatform.ergo.domain.{Block, ExtendedSettledTx, Output, SettledOutput, SettledTransaction, Transaction}
import tofu.streams.Emits
import tofu.syntax.monadic._
import tofu.syntax.streams.emits.emits

package object handlers {
  type BoxHandler[F[_]]          = F[Output] => F[Unit]
  type SettledBoxHandler[F[_]]   = F[SettledOutput] => F[Unit]
  type TxHandler[F[_]]           = F[Transaction] => F[Unit]
  type SettledTxHandler[F[_]]    = F[SettledTransaction] => F[Unit]
  type SettledBlockHandler[F[_]] = F[Block] => F[Unit]
  type ExtendedTxHandler[F[_]]   = F[ExtendedSettledTx] => F[Unit]

  def lift[F[_]: Functor](bh: BoxHandler[F]): SettledBoxHandler[F] = fa => bh(fa.map(_.output))

  def liftSettledTx[F[_]: Monad](f: F[SettledTransaction] => F[Unit]): ExtendedTxHandler[F] =
    stream =>
      f(stream.map(SettledTransaction.fromExtendedSettledTx))

  def liftOutputs[F[_]: Monad: Emits](f: BoxHandler[F]): ExtendedTxHandler[F] =
    stream =>
      f(stream.map(_.settledOutputs.map(_.output)).flatMap(emits(_)))

  def liftSettledOutputs[F[_]: Monad: Emits](f: SettledBoxHandler[F]): ExtendedTxHandler[F] =
    stream =>
      f(stream.map(_.settledOutputs).flatMap(emits(_)))
}
