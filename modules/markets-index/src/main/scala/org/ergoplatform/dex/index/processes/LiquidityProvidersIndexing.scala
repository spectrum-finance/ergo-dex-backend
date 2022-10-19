package org.ergoplatform.dex.index.processes

import cats.data.NonEmptyList
import cats.syntax.option._
import cats.syntax.traverse._
import cats.{Foldable, Functor, Monad}
import org.ergoplatform.dex.index.configs.StateIndexerConfig
import org.ergoplatform.dex.index.db.models.LiquidityProviderSnapshot
import org.ergoplatform.dex.index.repositories.LiquidityProvidersRepo
import org.ergoplatform.dex.protocol.ErgoTreeSerializer
import org.ergoplatform.dex.protocol.amm.constants
import org.ergoplatform.dex.protocol.constants.ErgoAssetId
import org.ergoplatform.dex.tracker.handlers.ExtendedTxHandler
import org.ergoplatform.ergo.SErgoTree
import org.ergoplatform.ergo.domain.BoxAsset
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import tofu.logging.{Logging, Logs}
import tofu.streams.{Chunks, Evals}
import tofu.syntax.logging._
import tofu.syntax.monadic._
import tofu.syntax.streams.evals._

import scala.math.BigDecimal.RoundingMode

trait LiquidityProvidersIndexing[F[_]] {
  def handler: ExtendedTxHandler[F]
}

object LiquidityProvidersIndexing {

  def make[
    I[_]: Functor,
    S[_]: Evals[*[_], F]: Chunks[*[_], C]: Monad,
    F[_]: Monad,
    C[_]: Functor: Foldable
  ](config: StateIndexerConfig)(implicit
    e: ErgoAddressEncoder,
    repo: LiquidityProvidersRepo[F],
    logs: Logs[I, F]
  ): I[ExtendedTxHandler[S]] =
    logs.forService[LiquidityProvidersIndexing[S]] map { implicit __ =>
      new Live[S, F, C](config).handler
    }

  final private class Live[
    S[_]: Evals[*[_], F]: Chunks[*[_], C]: Monad,
    F[_]: Monad: Logging,
    C[_]: Functor: Foldable
  ](config: StateIndexerConfig)(implicit e: ErgoAddressEncoder, repo: LiquidityProvidersRepo[F])
    extends LiquidityProvidersIndexing[S] {

    def handler: ExtendedTxHandler[S] = _.evalTap(tx => info"Going to process next tx ${tx.id}").flatMap { tx =>
      def makeAddress(ergoTree: SErgoTree): Option[String] = {
        val tree    = ErgoTreeSerializer.default.deserialize(ergoTree)
        val address = e.fromProposition(tree).toOption

        def isP2PK = address.exists {
          case _: P2PKAddress => true
          case _              => false
        }

        if (isP2PK) address.map(_.toString) else none
      }

      def process: F[Unit] = {
        val inputsF =
          tx.inputs.traverse { in =>
            makeAddress(in.output.ergoTree).traverse { address =>
              in.output.assets
                .flatMap(asset => config.pools.get(asset.tokenId).map(asset -> _))
                .traverse { case (BoxAsset(lpId, amount), poolId) =>
                  info"Got lp token $lpId of pool $poolId in input ${in.boxId} for address $address for txn ${tx.id} for block ${tx.blockId} with ts ${tx.timestamp}" >>
                    repo
                      .getLatestLiquidityProviderSnapshot(address, poolId)
                      .map(
                        _.getOrElse(
                          LiquidityProviderSnapshot.initial(
                            address,
                            poolId,
                            lpId,
                            in.output.boxId.value,
                            tx.timestamp,
                            tx.id.value,
                            tx.blockId.value,
                            tx.inclusionHeight
                          )
                        )
                      )
                      .flatMap { snapshot =>
                        repo
                          .getLatestPoolSnapshot(poolId, tx.inclusionHeight)
                          .flatMap { maybePool =>
                            val updatedSnapshot = maybePool
                              .map { pool =>
                                val gap     = tx.timestamp - snapshot.timestamp
                                val balance = snapshot.balance - amount
                                val lpPrice =
                                  ((if (pool.xId == ErgoAssetId.unwrapped)
                                      balance * pool.xAmount / (constants.cfmm.TotalEmissionLP - pool.lpAmount)
                                    else
                                      balance * pool.yAmount / (constants.cfmm.TotalEmissionLP - pool.lpAmount)) * 2 / BigDecimal(
                                    10
                                  ).pow(9)).setScale(9, RoundingMode.HALF_UP)
                                val weight = gap * snapshot.lpErg

                                snapshot.copy(
                                  boxId       = in.output.boxId.value,
                                  txId        = tx.id.value,
                                  blockId     = tx.blockId.value,
                                  balance     = balance,
                                  timestamp   = tx.timestamp,
                                  weight      = weight,
                                  op          = s"withdraw",
                                  amount      = amount,
                                  gap         = gap,
                                  lpErg       = lpPrice,
                                  txHeight    = tx.inclusionHeight,
                                  poolStateId = pool.stateId
                                )
                              }
                              .getOrElse(
                                snapshot.copy(
                                  op        = "withdraw. No such pool",
                                  amount    = amount,
                                  boxId     = in.output.boxId.value,
                                  txId      = tx.id.value,
                                  blockId   = tx.blockId.value,
                                  txHeight  = tx.inclusionHeight,
                                  timestamp = tx.timestamp
                                )
                              )

                            repo.insert(NonEmptyList.one(updatedSnapshot)) >>= { c =>
                              info"Asset $lpId in input for address $address inserted. New snapshot is: $updatedSnapshot"
                            }
                          }
                      }
                }
                .void
            }.void
          }.void

        val outputsF =
          tx.settledOutputs.traverse { out =>
            makeAddress(out.output.ergoTree).traverse { address =>
              out.output.assets
                .flatMap(asset => config.pools.get(asset.tokenId).map(asset -> _))
                .traverse { case (BoxAsset(lpId, amount), poolId) =>
                  info"Got lp token $lpId of pool $poolId in output ${out.output.boxId} for address $address for txn ${tx.id} for block ${tx.blockId} with ts ${tx.timestamp}" >>
                    repo
                      .getLatestLiquidityProviderSnapshot(address, poolId)
                      .map(
                        _.getOrElse(
                          LiquidityProviderSnapshot.initial(
                            address,
                            poolId,
                            lpId,
                            out.output.boxId.value,
                            tx.timestamp,
                            tx.id.value,
                            tx.blockId.value,
                            tx.inclusionHeight
                          )
                        )
                      )
                      .flatMap { state =>
                        repo.getLatestPoolSnapshot(poolId, tx.inclusionHeight).flatMap { maybePool =>
                          val timeGap = tx.timestamp - state.timestamp
                          val balance = state.balance + amount

                          val updatedSnapshot = maybePool
                            .map { pool =>
                              val lpPrice =
                                ((if (pool.xId == ErgoAssetId.unwrapped)
                                    balance * pool.xAmount / (constants.cfmm.TotalEmissionLP - pool.lpAmount)
                                  else
                                    balance * pool.yAmount / (constants.cfmm.TotalEmissionLP - pool.lpAmount)) * 2 / BigDecimal(
                                  10
                                ).pow(9)).setScale(9, RoundingMode.HALF_UP)

                              val weight = timeGap * state.lpErg

                              state.copy(
                                balance     = balance,
                                timestamp   = tx.timestamp,
                                boxId       = out.output.boxId.value,
                                weight      = weight,
                                op          = s"deposit",
                                amount      = amount,
                                gap         = timeGap,
                                lpErg       = lpPrice,
                                txId        = tx.id.value,
                                txHeight    = tx.inclusionHeight,
                                blockId     = tx.blockId.value,
                                poolStateId = pool.stateId
                              )
                            }
                            .getOrElse(
                              state.copy(
                                op        = "deposit. No such pool",
                                amount    = amount,
                                boxId     = out.output.boxId.value,
                                txId      = tx.id.value,
                                blockId   = tx.blockId.value,
                                txHeight  = tx.inclusionHeight,
                                timestamp = tx.timestamp
                              )
                            )

                          repo.insert(NonEmptyList.one(updatedSnapshot)) >>= { c =>
                            info"Asset $lpId in input for address $address inserted. New snapshot is: $updatedSnapshot"
                          }
                        }
                      }
                }
                .void
            }.void
          }.void

        outputsF >> inputsF
      }

      eval(process >> info"Processing tx ${tx.id} finished.")
    }
  }
}
