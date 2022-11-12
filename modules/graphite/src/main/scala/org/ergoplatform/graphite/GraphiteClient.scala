package org.ergoplatform.graphite

import cats.effect.{Concurrent, ContextShift, Resource}
import cats.instances.list._
import cats.syntax.foldable._
import cats.syntax.show._
import cats.tagless.syntax.functorK._
import cats.{Apply, Monad}
import tofu.higherKind.{Mid, RepresentableK}
import tofu.lift.Lift
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

trait GraphiteClient[F[_]] {

  def send(point: GraphitePoint): F[Unit]

  def send(points: List[GraphitePoint]): F[Unit]
}

object GraphiteClient {

  implicit val representableK: RepresentableK[GraphiteClient] =
    tofu.higherKind.derived.genRepresentableK

  def make[I[_]: Concurrent: ContextShift: Lift[*[_], F], F[_]: Monad](
    settings: GraphiteSettings,
    transformation: PointTransformation
  )(implicit logs: Logs[I, F]): Resource[I, GraphiteClient[F]] =
    Client
      .make[I](settings)
      .map(_.mapK(Lift[I, F].liftF))
      .flatMap { client =>
        Resource.eval(logs.forService[GraphiteClient[F]]).map { implicit l =>
          new GraphiteClientTracing[F] attach new Live[F](client, transformation, settings.batchSize)
        }
      }

  final private class Live[F[_]: Monad](
    client: Client[F],
    transformation: PointTransformation,
    batchSize: Int
  ) extends GraphiteClient[F] {

    def send(point: GraphitePoint): F[Unit] =
      client.send(
        GraphitePoint
          .format(transformation(point))
          .getBytes(Encoding)
      )

    def send(points: List[GraphitePoint]): F[Unit] =
      points
        .grouped(batchSize)
        .toList
        .traverse_ { batch =>
          client.send(
            GraphitePoint
              .format(batch.map(transformation.apply))
              .getBytes(Encoding)
          )
        }
  }

  final private class GraphiteClientTracing[F[_]: Apply: Logging] extends GraphiteClient[Mid[F, *]] {

    def send(point: GraphitePoint): Mid[F, Unit] =
      trace"Sending graphite point: ${point.show}" *> _

    def send(points: List[GraphitePoint]): Mid[F, Unit] =
      trace"Sending graphite points: [${points.map(_.show).mkString(",")}]" *> _
  }

  private val Encoding = "UTF-8"
}
