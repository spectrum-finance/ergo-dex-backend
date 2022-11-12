package org.ergoplatform.graphite

import cats.effect.{Blocker, Concurrent, ContextShift, Resource, Sync}
import fs2.Chunk
import fs2.io.udp.{Packet, Socket, SocketGroup}
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
      socket  <- SocketGroup[F](blocker).flatMap(_.open())
    } yield new UdpClient[F](remote, socket)

  final class UdpClient[F[_]: Concurrent: ContextShift](
    remote: InetSocketAddress,
    socket: Socket[F]
  ) extends Client[F] {

    def send(message: Array[Byte]): F[Unit] =
      socket.write(Packet(remote, Chunk.array(message)))
  }
}
