package org.ergoplatform.dex.resolver.repositories

import cats.effect.concurrent.Ref
import cats.{FlatMap, Functor}
import derevo.derive
import io.circe.Json
import io.circe.syntax._
import org.ergoplatform.dex.domain.amm.state.{Confirmed, Predicted}
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.ergo.BoxId
import tofu.concurrent.MakeRef
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
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
  def existsPredicted(id: BoxId): F[Boolean]
}

object Pools {

  def make[I[_]: FlatMap, F[_]: FlatMap](implicit makeRef: MakeRef[I, F], logs: Logs[I, F]): I[Pools[F]] =
    for {
      store                      <- makeRef.refOf(Map.empty[String, Json])
      implicit0(log: Logging[F]) <- logs.forService[Pools[F]]
    } yield new PoolTracing[F] attach new InMemory[F](store)

  private def PredictedKey(id: BoxId)      = s"predicted:$id"
  private def LastPredictedKey(id: PoolId) = s"predicted:last:$id"
  private def LastConfirmedKey(id: PoolId) = s"confirmed:last:$id"

  final class InMemory[F[_]: Functor](store: Ref[F, Map[String, Json]]) extends Pools[F] {

    def getLastPredicted(id: PoolId): F[Option[Predicted[CFMMPool]]] =
      store.get.map(_.get(LastPredictedKey(id)) >>= (_.as[Predicted[CFMMPool]].toOption))

    def getLastConfirmed(id: PoolId): F[Option[Confirmed[CFMMPool]]] =
      store.get.map(_.get(LastConfirmedKey(id)) >>= (_.as[Confirmed[CFMMPool]].toOption))

    def put(pool: Predicted[CFMMPool]): F[Unit] =
      store.update {
        _.updated(LastPredictedKey(pool.predicted.poolId), pool.asJson)
          .updated(PredictedKey(pool.predicted.box.boxId), Json.Null)
      }

    def put(pool: Confirmed[CFMMPool]): F[Unit] =
      store.update(_.updated(LastConfirmedKey(pool.confirmed.poolId), pool.asJson))

    def existsPredicted(id: BoxId): F[Boolean] =
      store.get.map(_.contains(PredictedKey(id)))
  }

  final class PoolTracing[F[_]: FlatMap: Logging] extends Pools[Mid[F, *]] {

    def getLastPredicted(id: PoolId): Mid[F, Option[Predicted[CFMMPool]]] =
      _ >>= (r => trace"getLastPredicted(id=$id) = $r" as r)

    def getLastConfirmed(id: PoolId): Mid[F, Option[Confirmed[CFMMPool]]] =
      _ >>= (r => trace"getLastConfirmed(id=$id) = $r" as r)

    def put(pool: Predicted[CFMMPool]): Mid[F, Unit] =
      trace"put(pool=$pool)" *> _

    def put(pool: Confirmed[CFMMPool]): Mid[F, Unit] =
      trace"put(pool=$pool)" *> _

    def existsPredicted(id: BoxId): Mid[F, Boolean] =
      _ >>= (r => trace"existsPredicted(id=$id) = $r" as r)
  }
}
