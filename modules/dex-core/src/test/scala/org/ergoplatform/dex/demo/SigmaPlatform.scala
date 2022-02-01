package org.ergoplatform.dex.demo

import derevo.circe.decoder
import derevo.derive
import io.circe.Encoder
import org.bouncycastle.util.BigIntegers
import org.ergoplatform.ErgoBox.TokenId
import org.ergoplatform._
import org.ergoplatform.ergo.domain.Output
import scorex.crypto.hash.Digest32
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol
import sigmastate.basics.DLogProtocol.DLogProverInput
import sigmastate.eval.{CompiletimeIRContext, IRContext}
import sigmastate.lang.SigmaCompiler
import sttp.client3._
import sttp.client3.circe._
import sttp.client3.okhttp.OkHttpSyncBackend

@derive(decoder)
case class EpochInfo(height: Int)

trait SigmaPlatform {

  implicit def IR: IRContext                      = new CompiletimeIRContext()
  implicit def addressEncoder: ErgoAddressEncoder = ErgoAddressEncoder(ErgoAddressEncoder.MainnetNetworkPrefix)
  def sigma: SigmaCompiler                        = SigmaCompiler(ErgoAddressEncoder.MainnetNetworkPrefix)

  def secretHex: String

  def sk: DLogProverInput            = DLogProverInput(BigIntegers.fromUnsignedByteArray(Base16.decode(secretHex).get))
  def selfPk: DLogProtocol.ProveDlog = sk.publicImage
  def selfAddress: P2PKAddress       = P2PKAddress(selfPk)

  val minerFeeNErg = 1250000L

  def feeAddress: Pay2SAddress = Pay2SAddress(ErgoScriptPredef.feeProposition())
  def minerFeeBox              = new ErgoBoxCandidate(minerFeeNErg, feeAddress.script, currentHeight())

  def getToken(id: String, input: Output): (TokenId, Long) =
    (Digest32 @@ Base16.decode(id).get, input.assets.find(_.tokenId.unwrapped == id).map(_.amount).getOrElse(0L))

  def getToken(id: String, inputs: List[Output]): (TokenId, Long) =
    (Digest32 @@ Base16.decode(id).get, inputs.map(bx => getToken(id, bx)._2).sum)

  def extractTokens(input: Output): List[(TokenId, Long)] =
    input.assets.map(a => Digest32 @@ Base16.decode(a.tokenId.unwrapped).get -> a.amount)

  def extractTokens(inputs: List[Output]): List[(TokenId, Long)] =
    inputs
      .flatMap(_.assets.map(a => a.tokenId -> a.amount))
      .foldLeft(Map.empty[org.ergoplatform.ergo.TokenId, Long]) { case (acc, (tid, amt)) =>
        val accumulated = acc.getOrElse(tid, 0L)
        acc.updated(tid, accumulated + amt)
      }
      .toList
      .map { case (tokenId, amt) => Digest32 @@ Base16.decode(tokenId.unwrapped).get -> amt }

  private lazy val backend = OkHttpSyncBackend()

  def currentHeight(): Int =
    basicRequest
      .get(uri"https://api.ergoplatform.com/api/v1/epochs/params")
      .response(asJson[EpochInfo])
      .send(backend)
      .body
      .right
      .get
      .height

  def getInput(id: String): Option[Output] =
    basicRequest
      .get(uri"https://api.ergoplatform.com/api/v1/boxes/$id")
      .response(asJson[Output])
      .send(backend)
      .body
      .right
      .toOption

  def submitTx(tx: ErgoLikeTransaction)(implicit e: Encoder[ErgoLikeTransaction]): Either[String, String] =
    basicRequest
      .post(uri"http://165.227.26.175:16042/transactions")
      .body(tx)
      .response(asString)
      .send(backend)
      .body
}
