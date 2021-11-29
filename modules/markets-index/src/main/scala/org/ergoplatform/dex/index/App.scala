package org.ergoplatform.dex.index

import cats.effect.{Blocker, Resource}
import fs2.Chunk
import org.ergoplatform.common.EnvApp
import org.ergoplatform.common.db.{PostgresTransactor, doobieLogging}
import org.ergoplatform.common.streaming.{Consumer, Delayed}
import org.ergoplatform.dex.domain.amm.{CFMMOrder, OrderId}
import org.ergoplatform.dex.index.configs.ConfigBundle
import org.ergoplatform.dex.index.processes.Indexing
import org.ergoplatform.dex.index.repos.{AssetsRepo, CFMMOrdersRepo, OutputsRepo}
import org.ergoplatform.dex.protocol.orderbook.OrderContractFamily
import org.ergoplatform.ergo.ErgoNetworkStreaming
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.SttpBackend
import tofu.doobie.log.EmbeddableLogHandler
import tofu.doobie.transactor.Txr
import tofu.logging.Logs
import zio.{ExitCode, URIO, ZEnv}
import streaming._
import tofu.fs2Instances._
import tofu.lift.IsoK
import tofu.syntax.unlift._
import zio.interop.catz._


object App extends EnvApp[ConfigBundle] {



}
