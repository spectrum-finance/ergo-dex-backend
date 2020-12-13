package org.ergoplatform

import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import org.ergoplatform.dex.clients.StreamingErgoNetworkClient.ErgoExplorerNetworkClient
import org.ergoplatform.dex.configs.NetworkConfig
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend

object TestApp extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    init.use { client =>
      client.streamUnspentOutputs(10)
        .evalMap(out => IO(println(out)))
        .compile.drain as ExitCode.Success
    }

  def init: Resource[IO, ErgoExplorerNetworkClient[IO]] =
    for {
      blocker                                             <- Blocker[IO]
      implicit0(backend: SttpBackend[IO, Fs2Streams[IO]]) <- AsyncHttpClientFs2Backend.resource[IO](blocker)
      client = new ErgoExplorerNetworkClient[IO](NetworkConfig("api.ergoplatform.com"))
    } yield client
}
