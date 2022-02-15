package org.ergoplatform.dex.executor.amm.modules

import cats.Monad
import cats.effect.{Sync, Timer}
import cats.effect.concurrent.Ref
import org.ergoplatform.dex.domain.amm.{CFMMOrder, OrderId}
import tofu.concurrent.MakeRef
import tofu.generate.GenRandom
import tofu.syntax.monadic._

import scala.collection.immutable.{HashSet, TreeSet}
import scala.concurrent.duration.DurationInt

trait CFMMBacklog[F[_]] {

  /** Put an order to the backlog.
    */
  def put(order: CFMMOrder): F[Unit]

  def putLowPriority(order: CFMMOrder): F[Unit]

  /** Pop a candidate order for execution. Blocks until an order is available.
    */
  def pop: F[CFMMOrder]

  /** Put an order from the backlog.
    */
  def drop(id: OrderId): F[Boolean]
}

object CFMMBacklog {

  private val PollInterval     = 100.millis
  private val PriorityTreshold = 19
  private val PrioritySpace    = 99

  def make[I[_]: Sync, F[_]: Sync: Timer](implicit makeRef: MakeRef[I, F]): I[CFMMBacklog[F]] =
    for {
      implicit0(rnd: GenRandom[F]) <- GenRandom.instance[I, F]()
      candidatesR                  <- makeRef.refOf(TreeSet.empty[CFMMOrder])
      lpCandidatesR                <- makeRef.refOf(TreeSet.empty[CFMMOrder])
      survivorsR                   <- makeRef.refOf(HashSet.empty[OrderId])
    } yield new EphemeralCFMMBacklog(candidatesR, lpCandidatesR, survivorsR)

  // In-memory orders backlog.
  // Note: Not thread safe.
  final class EphemeralCFMMBacklog[F[_]: Monad: GenRandom](
    candidatesR: Ref[F, TreeSet[CFMMOrder]],
    lowPriorityCandidatesR: Ref[F, TreeSet[CFMMOrder]],
    survivorsR: Ref[F, HashSet[OrderId]]
  )(implicit T: Timer[F])
    extends CFMMBacklog[F] {

    def put(order: CFMMOrder): F[Unit] =
      candidatesR.update(_ + order) >> survivorsR.update(_ + order.id)

    def putLowPriority(order: CFMMOrder): F[Unit] =
      lowPriorityCandidatesR.update(_ + order) >> survivorsR.update(_ + order.id)

    def pop: F[CFMMOrder] = {
      def tryPop: F[CFMMOrder] =
        for {
          rnd <- GenRandom.nextInt(PrioritySpace)
          lpc <- lowPriorityCandidatesR.get.map(_.headOption)
          maybeWinner <- lpc match {
                           case Some(c) if rnd <= PriorityTreshold => Left(c).pure
                           case _                                  => candidatesR.get.map(xs => Right(xs.headOption))
                         }
          winner <- maybeWinner match {
                      case Right(Some(order)) => candidatesR.update(_ - order) as order
                      case Left(order)        => lowPriorityCandidatesR.update(_ - order) as order
                      case _                  => T.sleep(PollInterval) >> tryPop
                    }
        } yield winner
      for {
        c <- tryPop
        res <- survivorsR.get
                 .map(_.contains(c.id))
                 .ifM(survivorsR.update(_ - c.id) as c, pop)
      } yield res
    }

    def drop(id: OrderId): F[Boolean] =
      survivorsR.get.map(_.contains(id)).ifM(survivorsR.update(_ - id) as true, false.pure)
  }
}
