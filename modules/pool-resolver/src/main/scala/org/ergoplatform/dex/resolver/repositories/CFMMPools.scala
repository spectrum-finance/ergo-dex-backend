package org.ergoplatform.dex.resolver.repositories

import cats.effect.concurrent.Ref
import cats.effect.{BracketThrow, Concurrent, Timer}
import cats.{FlatMap, Functor, Parallel}
import derevo.derive
import io.circe.Json
import io.circe.syntax._
import io.github.oskin1.rocksdb.scodec.TxRocksDB
import org.ergoplatform.dex.domain.amm.{CFMMPool, PoolId}
import org.ergoplatform.ergo.BoxId
import org.ergoplatform.ergo.state.{ConfirmedIndexed, Predicted, Traced, Unconfirmed}
import scodec.Codec
import scodec.codecs.{bool, utf8}
import tofu.concurrent.MakeRef
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait CFMMPools[F[_]] {

  /** Get predicted state with the given `id`.
    */
  def getPrediction(id: BoxId): F[Option[Traced[Predicted[CFMMPool]]]]

  /** Get last predicted state of a pool with the given `id`.
    */
  def getLastPredicted(id: PoolId): F[Option[Predicted[CFMMPool]]]

  /** Get last confirmed state of a pool with the given `id`.
    */
  def getLastConfirmed(id: PoolId): F[Option[ConfirmedIndexed[CFMMPool]]]

  /** Get last unconfirmed state of a pool with the given `id`.
    */
  def getLastUnconfirmed(id: PoolId): F[Option[Unconfirmed[CFMMPool]]]

  /** Persist predicted pool.
    */
  def put(pool: Traced[Predicted[CFMMPool]]): F[Unit]

  /** Persist confirmed pool state.
    */
  def put(pool: ConfirmedIndexed[CFMMPool]): F[Unit]

  /** Persist unconfirmed pool state.
    */
  def put(pool: Unconfirmed[CFMMPool]): F[Unit]

  /** Invalidate non-confirmed state.
    */
  def invalidate(poolId: PoolId, id: BoxId): F[Unit]
}

object CFMMPools {

  def make[I[_]: FlatMap, F[_]: Parallel: Concurrent: Timer](implicit
    rocks: TxRocksDB[F],
    logs: Logs[I, F]
  ): I[CFMMPools[F]] =
    logs.forService[CFMMPools[F]].map { implicit l =>
      new CFMMPoolsTracing[F] attach new CFMMPoolsRocks[F]
    }

  def ephemeral[I[_]: FlatMap, F[_]: FlatMap](implicit makeRef: MakeRef[I, F], logs: Logs[I, F]): I[CFMMPools[F]] =
    for {
      store                      <- makeRef.refOf(Map.empty[String, Json])
      implicit0(log: Logging[F]) <- logs.forService[CFMMPools[F]]
    } yield new CFMMPoolsTracing[F] attach new InMemory[F](store)

  private def PredictedKey(id: BoxId)        = s"predicted:prevState:$id" // -> prevStateId
  private def LastPredictedKey(id: PoolId)   = s"predicted:last:$id" // -> state
  private def LastConfirmedKey(id: PoolId)   = s"confirmed:last:$id"
  private def LastUnconfirmedKey(id: PoolId) = s"confirmed:last:$id"

  final class CFMMPoolsRocks[F[_]: BracketThrow](implicit rocks: TxRocksDB[F]) extends CFMMPools[F] {

    implicit val codecString: Codec[String] = utf8
    implicit val codecBool: Codec[Boolean]  = bool

    def getPrediction(id: BoxId): F[Option[Traced[Predicted[CFMMPool]]]] = ???

    def getLastPredicted(id: PoolId): F[Option[Predicted[CFMMPool]]] =
      rocks.get[String, Predicted[CFMMPool]](LastPredictedKey(id))

    def getLastConfirmed(id: PoolId): F[Option[ConfirmedIndexed[CFMMPool]]] =
      rocks.get[String, ConfirmedIndexed[CFMMPool]](LastConfirmedKey(id))

    def getLastUnconfirmed(id: PoolId): F[Option[Unconfirmed[CFMMPool]]] =
      rocks.get[String, Unconfirmed[CFMMPool]](LastUnconfirmedKey(id))

    def put(tracedPool: Traced[Predicted[CFMMPool]]): F[Unit] =
      rocks.beginTransaction.use { tx =>
        for {
          _ <- tx.put(LastPredictedKey(tracedPool.state.entity.poolId), tracedPool.state)
          _ <- tx.put(PredictedKey(tracedPool.state.entity.box.boxId), tracedPool)
          _ <- tx.commit
        } yield ()
      }

    def put(pool: ConfirmedIndexed[CFMMPool]): F[Unit] =
      rocks.put(LastConfirmedKey(pool.entity.poolId), pool)

    def put(pool: Unconfirmed[CFMMPool]): F[Unit] =
      rocks.put(LastUnconfirmedKey(pool.entity.poolId), pool)

    def invalidate(poolId: PoolId, stateId: BoxId): F[Unit] =
      rocks.beginTransaction.use { tx =>
        for {
          lastPredictedState <- tx.get[String, Predicted[CFMMPool]](LastPredictedKey(poolId))
          _ <- lastPredictedState match {
                 case Some(Predicted(predicted)) if predicted.box.boxId == stateId =>
                   tx.delete(LastPredictedKey(poolId))
                 case _ => unit
               }
          lastUnconfirmedState <- tx.get[String, Unconfirmed[CFMMPool]](LastUnconfirmedKey(poolId))
          _ <- lastUnconfirmedState match {
                 case Some(Unconfirmed(unconfirmed)) if unconfirmed.box.boxId == stateId =>
                   tx.delete(LastUnconfirmedKey(poolId))
                 case _ => unit
               }
          _ <- tx.delete(PredictedKey(stateId))
          _ <- tx.commit
        } yield ()
      }
  }

  final class InMemory[F[_]: Functor](store: Ref[F, Map[String, Json]]) extends CFMMPools[F] {

    def getPrediction(id: BoxId): F[Option[Traced[Predicted[CFMMPool]]]] = ???

    def getLastPredicted(id: PoolId): F[Option[Predicted[CFMMPool]]] =
      store.get.map(_.get(LastPredictedKey(id)) >>= (_.as[Predicted[CFMMPool]].toOption))

    def getLastConfirmed(id: PoolId): F[Option[ConfirmedIndexed[CFMMPool]]] =
      store.get.map(_.get(LastConfirmedKey(id)) >>= (_.as[ConfirmedIndexed[CFMMPool]].toOption))

    def getLastUnconfirmed(id: PoolId): F[Option[Unconfirmed[CFMMPool]]] =
      store.get.map(_.get(LastUnconfirmedKey(id)) >>= (_.as[Unconfirmed[CFMMPool]].toOption))

    def put(pool: Traced[Predicted[CFMMPool]]): F[Unit] =
      store.update {
        _.updated(LastPredictedKey(pool.state.entity.poolId), pool.asJson)
          .updated(PredictedKey(pool.state.entity.box.boxId), Json.Null)
      }

    def put(pool: ConfirmedIndexed[CFMMPool]): F[Unit] =
      store.update(_.updated(LastConfirmedKey(pool.entity.poolId), pool.asJson))

    def put(pool: Unconfirmed[CFMMPool]): F[Unit] =
      store.update(_.updated(LastUnconfirmedKey(pool.entity.poolId), pool.asJson))

    def invalidate(poolId: PoolId, stateId: BoxId): F[Unit] =
      store.update(_ - stateId.value)
  }

  final class CFMMPoolsTracing[F[_]: FlatMap: Logging] extends CFMMPools[Mid[F, *]] {

    def getPrediction(id: BoxId): Mid[F, Option[Traced[Predicted[CFMMPool]]]] =
      for {
        _ <- trace"getPrediction(id=$id)"
        r <- _
        _ <- trace"getPrediction(id=$id) -> $r"
      } yield r

    def getLastPredicted(id: PoolId): Mid[F, Option[Predicted[CFMMPool]]] =
      for {
        _ <- trace"getLastPredicted(id=$id)"
        r <- _
        _ <- trace"getLastPredicted(id=$id) -> $r"
      } yield r

    def getLastConfirmed(id: PoolId): Mid[F, Option[ConfirmedIndexed[CFMMPool]]] =
      for {
        _ <- trace"getLastConfirmed(id=$id)"
        r <- _
        _ <- trace"getLastConfirmed(id=$id) -> $r"
      } yield r

    def getLastUnconfirmed(id: PoolId): Mid[F, Option[Unconfirmed[CFMMPool]]] =
      for {
        _ <- trace"getLastConfirmed(id=$id)"
        r <- _
        _ <- trace"getLastConfirmed(id=$id) -> $r"
      } yield r

    def put(pool: Traced[Predicted[CFMMPool]]): Mid[F, Unit] =
      for {
        _ <- trace"put(pool=$pool)"
        r <- _
        _ <- trace"put(pool=$pool) -> $r"
      } yield r

    def put(pool: ConfirmedIndexed[CFMMPool]): Mid[F, Unit] =
      for {
        _ <- trace"put(pool=$pool)"
        r <- _
        _ <- trace"put(pool=$pool) -> $r"
      } yield r

    def put(pool: Unconfirmed[CFMMPool]): Mid[F, Unit] =
      for {
        _ <- trace"put(pool=$pool)"
        r <- _
        _ <- trace"put(pool=$pool) -> $r"
      } yield r

    def invalidate(poolId: PoolId, stateId: BoxId): Mid[F, Unit] =
      for {
        _ <- trace"dropPrediction(poolId=$poolId, stateId=$stateId)"
        r <- _
        _ <- trace"dropPrediction(poolId=$poolId, stateId=$stateId) -> $r"
      } yield r
  }
}
