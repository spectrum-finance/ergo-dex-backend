package org.ergoplatform.dex.resolver.repositories

import cats.Functor
import cats.effect.concurrent.Ref
import io.circe.Json
import io.circe.syntax._
import org.ergoplatform.dex.domain.amm.state.{Confirmed, Predicted}
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import tofu.concurrent.MakeRef
import tofu.syntax.monadic._

trait Pools[F[_]] {

  /** Get last predicted state of a pool with the given `id`.
    */
  def getLastPredicted(id: PoolId): F[Option[Predicted[CFMMPool]]]

  /** Get last confirmed state of a pool with the given `id`.
    */
  def getLastConfirmed(id: PoolId): F[Option[Confirmed[CFMMPool]]]

  /** Persist predicted pool.
    */
  def put(pool: Predicted[CFMMPool]): F[Unit]

  /** Persist confirmed pool state.
    */
  def put(pool: Confirmed[CFMMPool]): F[Unit]

  /** Check whether a pool state prediction with the given `id` exists.
    */
  def existsPredicted(id: PoolId): F[Boolean]
}

object Pools {

  def make[I[_]: Functor, F[_]: Functor](implicit makeRef: MakeRef[I, F]): I[Pools[F]] =
    makeRef.refOf(Map.empty[String, Json]).map(store => new InMemory[F](store))

  private def PredictedKey(id: PoolId) = s"predicted:$id"
  private def LastPredictedKey(id: PoolId) = s"predicted:last:$id"
  private def LastConfirmedKey(id: PoolId) = s"confirmed:last:$id"

  final class InMemory[F[_]: Functor](store: Ref[F, Map[String, Json]]) extends Pools[F] {

    def getLastPredicted(id: PoolId): F[Option[Predicted[CFMMPool]]] =
      store.get.map(_.get(LastPredictedKey(id)) >>= (_.as[Predicted[CFMMPool]].toOption))

    def getLastConfirmed(id: PoolId): F[Option[Confirmed[CFMMPool]]] =
      store.get.map(_.get(LastConfirmedKey(id)) >>= (_.as[Confirmed[CFMMPool]].toOption))

    def put(pool: Predicted[CFMMPool]): F[Unit] =
      store.update(_.updated(LastPredictedKey(pool.predicted.poolId), pool.asJson))

    def put(pool: Confirmed[CFMMPool]): F[Unit] =
      store.update(_.updated(LastConfirmedKey(pool.confirmed.poolId), pool.asJson))

    def existsPredicted(id: PoolId): F[Boolean] =
      store.get.map(_.contains(PredictedKey(id)))
  }
}
