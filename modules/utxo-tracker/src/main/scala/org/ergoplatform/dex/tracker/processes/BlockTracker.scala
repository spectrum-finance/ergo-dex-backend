package org.ergoplatform.dex.tracker.processes

import cats.{Defer, Functor, Monad, MonoidK}
import derevo.derive
import org.ergoplatform.dex.tracker.configs.BlockTrackerConfig
import org.ergoplatform.dex.tracker.handlers.SettledBlockHandler
import org.ergoplatform.dex.tracker.repositories.TrackerCache
import org.ergoplatform.ergo.domain.SettledBlock
import org.ergoplatform.ergo.modules.{ErgoNetwork, LedgerStreaming}
import tofu.Catches
import tofu.higherKind.derived.representableK
import tofu.logging.{Logging, Logs}
import tofu.streams.{Evals, Pace, ParFlatten}
import tofu.syntax.handle._
import tofu.syntax.embed._
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.all._

@derive(representableK)
trait BlockTracker[F[_]] {

  def run: F[Unit]
}

object BlockTracker {

  def make[
    I[_]: Functor,
    F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: Catches: BlockTrackerConfig.Has,
    G[_]: Monad
  ](handlers: SettledBlockHandler[F]*)(implicit
    network: ErgoNetwork[G],
    cache: TrackerCache[G],
    ledger: LedgerStreaming[F],
    logs: Logs[I, G]
  ): I[BlockTracker[F]] =
    logs.forService[BlockTracker[F]].map { implicit l =>
      (BlockTrackerConfig.access map (conf =>
        new StreamingBlockTracker[F, G](conf, handlers.toList): BlockTracker[F]
      )).embed
    }

  final class StreamingBlockTracker[
    F[_]: Monad: Evals[*[_], G]: ParFlatten: Pace: Defer: MonoidK: Catches,
    G[_]: Monad: Logging
  ](conf: BlockTrackerConfig, handlers: List[SettledBlockHandler[F]])(implicit
    cache: TrackerCache[G],
    ledger: LedgerStreaming[F],
    network: ErgoNetwork[G]
  ) extends BlockTracker[F] {

    def run: F[Unit] =
      eval(info"Starting block tracker ..") >>
      eval(cache.lastScannedBlockOffset).repeat
        .flatMap { lastOffset =>
          eval(network.getNetworkInfo).flatMap { networkParams =>
            val offset     = lastOffset max conf.initialOffset
            val maxOffset  = networkParams.height
            val nextOffset = (offset + conf.batchSize) min maxOffset
            val scan =
              eval(
                info"Requesting block batch {offset=$offset, maxOffset=$maxOffset, batchSize=${conf.batchSize} .."
              ) >>
              ledger
                .streamBlocks(offset, conf.batchSize)
                .evalTap(block => trace"Scanning block $block")
                .flatTap(block => emits(handlers.map(_(block.pure[F]))).parFlattenUnbounded)
                .evalMap(block => cache.setLastScannedBlockOffset(block.height))
            val finalizeOffset = eval(cache.setLastScannedBlockOffset(nextOffset))
            val pause =
              eval(info"Upper limit {maxOffset=$maxOffset} was reached. Retrying in ${conf.retryDelay.toSeconds}s") >>
              unit[F].delay(conf.retryDelay)

            emits(if (offset != maxOffset) List(scan, finalizeOffset) else List(pause)).flatten
          }
        }
        .handleWith[Throwable] { e =>
          val delay = conf.retryDelay
          eval(warnCause"Tracker failed. Retrying in $delay ms" (e)) >> run.delay(delay)
        }
  }
}
