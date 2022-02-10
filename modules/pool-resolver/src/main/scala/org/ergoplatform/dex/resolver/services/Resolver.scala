package org.ergoplatform.dex.resolver.services

import cats.{Functor, Monad}
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.dex.resolver.repositories.CFMMPools
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.state.{ConfirmedIndexed, Predicted, Traced, Unconfirmed}
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

trait Resolver[F[_]] {

  /** Get pool state by pool id.
    */
  def resolve(id: PoolId): F[Option[CFMMPool]]

  /** Persist predicted pool.
    */
  def put(pool: Traced[Predicted[CFMMPool]]): F[Unit]

  /** Invalidate pool state.
    */
  def invalidate(poolId: PoolId, boxId: BoxId): F[Unit]
}

object Resolver {

  def make[I[+_]: Functor, F[_]: Monad](implicit pools: CFMMPools[F], logs: Logs[I, F]): I[Resolver[F]] =
    logs.forService[Resolver[F]] map (implicit l => new Live[F])

  final class Live[F[_]: Monad: Logging](implicit pools: CFMMPools[F]) extends Resolver[F] {

    def resolve(id: PoolId): F[Option[CFMMPool]] =
      for {
        confirmedOpt   <- pools.getLastConfirmed(id)
        unconfirmedOpt <- pools.getLastUnconfirmed(id)
        predictedOpt   <- pools.getLastPredicted(id)
        pool <- (confirmedOpt, unconfirmedOpt, predictedOpt) match {
                  case (Some(ConfirmedIndexed(confirmed, _)), unconfirmedMaybe, Some(Predicted(predicted))) =>
                    // 1. Select anchoring point: mempool version is in priority
                    val anchoringPoint             = unconfirmedMaybe.map(_.entity).getOrElse(confirmed)
                    val predictionIsAnchoringPoint = anchoringPoint.box.boxId == predicted.box.boxId
                    // 2. Try to trace back anchoring point
                    def trace(stateId: BoxId): F[Boolean] =
                      pools.getPrediction(stateId).flatMap {
                        case Some(Traced(_, predBoxId)) if predBoxId == anchoringPoint.box.boxId =>
                          true.pure
                        case Some(Traced(_, predecessorBoxId)) => trace(predecessorBoxId)
                        case None                              => false.pure
                      }
                    for {
                      predictionIsValid <- if (predictionIsAnchoringPoint) true.pure
                                           else trace(predicted.box.boxId)
                      pool = if (predictionIsValid) predicted
                             else anchoringPoint
                    } yield Some(pool)
                  case (_, Some(Unconfirmed(unconfirmed)), None) =>
                    debug"Falling back to the last unconfirmed state for Pool{id='$id'}" as Some(unconfirmed)
                  case (Some(ConfirmedIndexed(confirmed, _)), None, None) =>
                    debug"Falling back to the last confirmed state for Pool{id='$id'}" as Some(confirmed)
                  case _ =>
                    warn"Cannot resolve Pool{id='$id'}" as None
                }
      } yield pool

    def put(pool: Traced[Predicted[CFMMPool]]): F[Unit] =
      debug"New prediction for Pool{id='${pool.state.entity.poolId}'}, $pool" >>
      pools.put(pool)

    def invalidate(poolId: PoolId, boxId: BoxId): F[Unit] =
      debug"Invalidating PoolState{boxId=$boxId}" >>
      pools.invalidate(poolId, boxId)
  }
}
