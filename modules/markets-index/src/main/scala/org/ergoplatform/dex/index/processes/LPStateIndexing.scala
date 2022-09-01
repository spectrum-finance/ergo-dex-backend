package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.{Foldable, Functor, Monad}
import org.ergoplatform.dex.index.streaming.{LqLocksConsumer, TxnsConsumer}
import tofu.streams.{Chunks, Evals}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.chunks._
import cats.syntax.traverse._
import tofu.syntax.streams.evals._
import cats.syntax.foldable._
import cats.syntax.option.none
import org.ergoplatform.{ErgoAddress, ErgoAddressEncoder, P2PKAddress}
import org.ergoplatform.dex.index.configs.StateIndexerConfig
import org.ergoplatform.dex.index.db.models.DBLpState
import org.ergoplatform.dex.index.processes.LocksIndexing.Live
import org.ergoplatform.dex.index.repositories.{LPStateRepo, RepoBundle}
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.constants
import org.ergoplatform.dex.protocol.constants.ErgoAssetId
import org.ergoplatform.dex.tracker.handlers.ExtendedTxHandler
import org.ergoplatform.ergo.{ErgoTreeTemplate, SErgoTree}
import tofu.doobie.transactor.Txr
import tofu.logging.{Logging, Logs}

import scala.math.BigDecimal.RoundingMode

trait LPStateIndexing[F[_]] {
  def handler: ExtendedTxHandler[F]
}

object LPStateIndexing {

  def make[
    I[_]: Functor,
    S[_]: Evals[*[_], F]: Chunks[*[_], C]: Monad,
    F[_]: Monad,
    C[_]: Functor: Foldable
  ](config: StateIndexerConfig)(implicit
    e: ErgoAddressEncoder,
    repo: LPStateRepo[F],
    logs: Logs[I, F]
  ): I[LPStateIndexing[S]] =
    logs.forService[LPStateIndexing[S]] map { implicit __ =>
      new Live[S, F, C](config)
    }

  final private class Live[
    S[_]: Evals[*[_], F]: Chunks[*[_], C]: Monad,
    F[_]: Monad: Logging,
    C[_]: Functor: Foldable
  ](config: StateIndexerConfig)(implicit e: ErgoAddressEncoder, repo: LPStateRepo[F])
    extends LPStateIndexing[S] {

    def handler: ExtendedTxHandler[S] = _.evalTap(tx => info"Going to process next tx ${tx.id}").flatMap { tx =>
      def makeAddress(ergoTree: SErgoTree): Option[(String, Boolean)] = {
        val tree    = ErgoTreeSerializer.default.deserialize(ergoTree)
        val address = e.fromProposition(tree).toOption

        def isP2PK = address.exists {
          case _: P2PKAddress => true
          case _              => false
        }

        address.map(a => a.toString -> isP2PK)
      }

      def process: F[Unit] = {
        val inputsF =
          tx.inputs.traverse { in =>
            makeAddress(in.output.ergoTree).traverse {
              case (_, false) => unit[F]
              case (address, _) =>
                val assets = in.output.assets.filter(asset => config.tokensIds.contains(asset.tokenId.unwrapped))
                assets.traverse { asset =>
                  info"Got next asset $asset in input ${in.boxId} for address $address for txn ${tx.id} for block ${tx.blockId} ts ${tx.timestamp}" >>
                  repo
                    .getPreviousState(address, asset.tokenId.unwrapped)
                    .map(
                      _.getOrElse(
                        DBLpState.initial(
                          address,
                          asset.tokenId.unwrapped,
                          in.output.boxId.value,
                          tx.timestamp,
                          tx.id.value,
                          tx.blockId.value,
                          tx.inclusionHeight
                        )
                      )
                    )
                    .flatMap { state =>
                      repo.getLatestPoolState((asset.tokenId.unwrapped), tx.inclusionHeight).flatMap { maybePool =>
                        val newState = maybePool
                          .map { pool =>
                            val timeGap = tx.timestamp - state.timestamp
                            val balance = state.balance - asset.amount

                            val lpPrice =
                              ((if (pool.xId == ErgoAssetId.unwrapped)
                                  balance * pool.xAmount / (constants.cfmm.TotalEmissionLP - pool.lpAmount)
                                else
                                  balance * pool.yAmount / (constants.cfmm.TotalEmissionLP - pool.lpAmount)) * 2 / 1000000000)
                                .setScale(9, RoundingMode.HALF_UP)

                            val weight = timeGap * state.lpErg
                            val newState =
                              state.copy(
                                balance     = balance,
                                timestamp   = tx.timestamp,
                                boxId       = in.output.boxId.value,
                                weight      = weight,
                                op          = s"withdraw",
                                amount      = asset.amount,
                                gap         = timeGap,
                                lpErg       = lpPrice,
                                txId        = tx.id.value,
                                blockId     = tx.blockId.value,
                                poolStateId = pool.stateId
                              )
                            newState
                          }
                          .getOrElse(state.copy(op = "No such pool"))

                        repo.insert(NonEmptyList.one(newState)) >>= { c =>
                          info"Asset $asset in input for address $address inserted: $c with new state $newState"
                        }
                      }
                    }
                }.void
            }.void
          }.void

        val outputsF =
          tx.settledOutputs.traverse { out =>
            makeAddress(out.output.ergoTree).traverse {
              case (_, false) => unit[F]
              case (address, _) =>
                val assets = out.output.assets.filter(asset => config.tokensIds.contains(asset.tokenId.unwrapped))
                assets.traverse { asset =>
                  info"Got next asset $asset in output ${out.output.boxId} for address $address for txn ${tx.id} for block ${tx.blockId} ts ${tx.timestamp}" >>
                  repo
                    .getPreviousState(address, asset.tokenId.unwrapped)
                    .map(
                      _.getOrElse(
                        DBLpState.initial(
                          address,
                          asset.tokenId.unwrapped,
                          out.output.boxId.value,
                          tx.timestamp,
                          tx.id.value,
                          tx.blockId.value,
                          tx.inclusionHeight
                        )
                      )
                    )
                    .flatMap { state =>
                      repo.getLatestPoolState((asset.tokenId.unwrapped), tx.inclusionHeight).flatMap { maybePool =>
                        val timeGap = tx.timestamp - state.timestamp
                        val balance = state.balance + asset.amount

                        val newState = maybePool
                          .map { pool =>
                            val lpPrice =
                              ((if (pool.xId == ErgoAssetId.unwrapped)
                                  balance * pool.xAmount / (constants.cfmm.TotalEmissionLP - pool.lpAmount)
                                else
                                  balance * pool.yAmount / (constants.cfmm.TotalEmissionLP - pool.lpAmount)) * 2 / 1000000000)
                                .setScale(9, RoundingMode.HALF_UP)

                            val weight = timeGap * state.lpErg

                            val newState =
                              state.copy(
                                balance     = balance,
                                timestamp   = tx.timestamp,
                                boxId       = out.output.boxId.value,
                                weight      = weight,
                                op          = s"deposit",
                                amount      = asset.amount,
                                gap         = timeGap,
                                lpErg       = lpPrice,
                                txId        = tx.id.value,
                                blockId     = tx.blockId.value,
                                poolStateId = pool.stateId
                              )
                            newState
                          }
                          .getOrElse(state.copy(op = "No such pool"))

                        repo.insert(NonEmptyList.one(newState)) >>= { c =>
                          info"Asset $asset in output for address $address inserted: $c with new state $newState"
                        }
                      }
                    }
                }.void
            }.void
          }.void

        inputsF >> outputsF
      }

      eval(process >> info"Processing tx ${tx.id} finished.")
    }
  }
}
