package org.ergoplatform.graphite

import cats.effect.{Concurrent, ContextShift, Resource}
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
}

object GraphiteClient {

  implicit val representableK: RepresentableK[GraphiteClient] =
    tofu.higherKind.derived.genRepresentableK

  def make[I[_]: Concurrent: ContextShift: Lift[*[_], F], F[_]: Monad](
    settings: GraphiteSettings,
    prefix: String
  )(implicit logs: Logs[I, F]): Resource[I, GraphiteClient[F]] =
    Client
      .make[I](settings)
      .map(_.mapK(Lift[I, F].liftF))
      .flatMap { client =>
        Resource.eval(logs.forService[GraphiteClient[F]]).map { implicit l =>
          new GraphiteClientTracing[F] attach new Live[F](client, prefix)
        }
      }

  final private class Live[F[_]: Monad](
    client: Client[F],
    prefix: String
  ) extends GraphiteClient[F] {

    def send(point: GraphitePoint): F[Unit] =
      client.send(
        point
          .transformation(prefix)
          .format
          .getBytes(Encoding)
      )
  }

  final private class GraphiteClientTracing[F[_]: Apply: Logging] extends GraphiteClient[Mid[F, *]] {

    def send(point: GraphitePoint): Mid[F, Unit] =
      trace"Sending graphite point: ${point.show}" *> _
  }

  private val Encoding = "UTF-8"
}
