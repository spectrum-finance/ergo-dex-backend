package org.ergoplatform.graphite

import cats.effect.{Blocker, Concurrent, ContextShift, Resource, Sync}
import fs2.Chunk
import fs2.io.udp.{Packet, SocketGroup}
import tofu.higherKind.RepresentableK

import java.net.InetSocketAddress

trait Client[F[_]] {

  def send(message: Array[Byte]): F[Unit]
}

object Client {

  implicit val representableK: RepresentableK[Client] =
    tofu.higherKind.derived.genRepresentableK

  def make[F[_]: Concurrent: ContextShift](
    settings: GraphiteSettings
  ): Resource[F, Client[F]] =
    for {
      blocker <- Blocker[F]
      remote  <- Resource.eval(Sync[F].delay(new InetSocketAddress(settings.host, settings.port)))
    } yield new UdpClient[F](remote, blocker)

  final class UdpClient[F[_]: Concurrent: ContextShift](
    remote: InetSocketAddress,
    blocker: Blocker
  ) extends Client[F] {

    def send(message: Array[Byte]): F[Unit] =
      SocketGroup[F](blocker)
        .flatMap(_.open())
        .use(socket => socket.write(Packet(remote, Chunk.array(message))))
  }
}
