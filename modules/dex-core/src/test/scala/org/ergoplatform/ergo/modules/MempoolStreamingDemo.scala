package org.ergoplatform.ergo.modules

import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import org.ergoplatform.dex.configs.NetworkConfig
import org.ergoplatform.ergo.services.node.ErgoNode
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.client3.{SttpBackend, UriContext}
import tofu.Context
import tofu.fs2Instances._
import tofu.logging.Logs
import tofu.syntax.monadic._

object MempoolStreamingDemo extends IOApp {

  implicit val logs: Logs[IO, IO] = Logs.sync[IO, IO]

  def run(args: List[String]): IO[ExitCode] =
    init.use { streaming =>
      streaming.streamUnspentOutputs.evalMap(o => IO.delay(println(s"New output: $o"))).compile.drain >>
      IO.delay(println(s"Done")) as ExitCode.Success
    }

  def init =
    for {
      blocker                                  <- Blocker[IO]
      implicit0(backend: SttpBackend[IO, Any]) <- AsyncHttpClientFs2Backend.resource[IO](blocker)
      implicit0(hasConf: NetworkConfig.Has[IO]) =
        Context.const[IO, NetworkConfig](NetworkConfig(uri"https://api.ergoplatform.com", uri"http://localhost:9053"))
      implicit0(node: ErgoNode[IO]) <- Resource.eval(ErgoNode.make[IO, IO])
      streaming = MempoolStreaming.make[fs2.Stream[IO, *], IO]
    } yield streaming
}
