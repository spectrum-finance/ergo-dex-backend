package org.ergoplatform.dex.executor.amm.modules

import cats.Monad
import cats.effect.Timer
import cats.effect.concurrent.Ref
import org.ergoplatform.dex.domain.amm.{CFMMOrder, OrderId}
import tofu.concurrent.MakeRef
import tofu.syntax.monadic._

import scala.collection.immutable.{HashSet, TreeSet}
import scala.concurrent.duration.DurationInt

trait CFMMBacklog[F[_]] {

  /** Put an order to the backlog.
    */
  def put(order: CFMMOrder): F[Unit]

  /** Pop a candidate order for execution. Blocks until an order is available.
    */
  def pop: F[CFMMOrder]

  /** Put an order from the backlog.
    */
  def drop(id: OrderId): F[Unit]
}

object CFMMBacklog {

  private val PollInterval = 1.second

  def make[I[_]: Monad, F[_]: Monad: Timer](implicit makeRef: MakeRef[I, F]): I[CFMMBacklog[F]] =
    for {
      candidatesR <- makeRef.refOf(TreeSet.empty[CFMMOrder])
      survivorsR  <- makeRef.refOf(HashSet.empty[OrderId])
    } yield new EphemeralCFMMBacklog(candidatesR, survivorsR)

  // In-memory orders backlog.
  // Note: Not thread safe.
  final class EphemeralCFMMBacklog[F[_]: Monad](
    candidatesR: Ref[F, TreeSet[CFMMOrder]],
    survivorsR: Ref[F, HashSet[OrderId]]
  )(implicit T: Timer[F])
    extends CFMMBacklog[F] {

    def put(order: CFMMOrder): F[Unit] =
      candidatesR.update(_ + order) >> survivorsR.update(_ + order.id)

    def pop: F[CFMMOrder] = {
      def tryPop: F[CFMMOrder] =
        candidatesR.get.map(_.headOption).flatMap {
          case Some(order) => candidatesR.update(_ - order) as order
          case None        => T.sleep(PollInterval) >> tryPop
        }
      for {
        c <- tryPop
        res <- survivorsR.get
                 .map(_.contains(c.id))
                 .ifM(survivorsR.update(_ - c.id) as c, pop)
      } yield res
    }

    def drop(id: OrderId): F[Unit] = survivorsR.update(_ - id)
  }
}
