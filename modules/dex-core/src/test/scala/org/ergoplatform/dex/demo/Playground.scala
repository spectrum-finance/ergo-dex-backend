package org.ergoplatform.dex.demo

import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.dex.contracts.amm.cfmm.t2t.{DepositContract, RedeemContract, SwapContract}
import sigmastate.Values.ErgoTree
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval.{CompiletimeIRContext, IRContext}
import sigmastate.lang.SigmaCompiler
import sigmastate.lang.Terms.ValueOps

object Playground extends App {

  private val dummyPk       = DLogProverInput(BigInt(Long.MaxValue).bigInteger).publicImage
  private val dummyDigest32 = Array.fill(32)(0: Byte)
  private val dummyLong     = 1000L

  implicit private val IR: IRContext = new CompiletimeIRContext()
  val sigma                          = SigmaCompiler(ErgoAddressEncoder.MainnetNetworkPrefix)

  val env = Map(
    "Pk"             -> dummyPk,
    "PoolNFT"        -> Array.fill(32)(1: Byte),
    "DexFee"         -> 44L
  )

  val tree = ErgoTree.fromProposition(sigma.compile(env, RedeemContract).asSigmaProp)

  tree.constants.zipWithIndex.map { case (v, idx) => idx -> v }.foreach(println)
}
