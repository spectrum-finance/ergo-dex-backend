package org.ergoplatform.dex.domain.amm

import derevo.circe.{decoder, encoder}
import derevo.derive
import io.circe.{Decoder, Encoder}
import org.ergoplatform.dex.domain.AssetAmount
import org.ergoplatform.dex.protocol.codecs.{ergoLikeTransactionDecoder, ergoLikeTransactionEncoder}
import org.ergoplatform.ergo.domain.Output
import scodec.Codec
import scodec.codecs.{int64, int8}
import tofu.logging.derivation.loggable

@derive(encoder, decoder, loggable)
sealed trait CFMMOrder {
  val poolId: PoolId
  val maxMinerFee: Long
  val box: Output
  val timestamp: Long

  def id: OrderId = OrderId.fromBoxId(box.boxId)
}

@derive(encoder, decoder, loggable)
final case class Deposit(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: DepositParams, box: Output)
  extends CFMMOrder

object Deposit {

  implicit val codec: Codec[Deposit] =
    (implicitly[Codec[PoolId]] ::
      int64 ::
      int64 ::
      implicitly[Codec[DepositParams]] ::
      implicitly[Codec[Output]]).as[Deposit]
}

@derive(encoder, decoder, loggable)
final case class Redeem(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: RedeemParams, box: Output)
  extends CFMMOrder

object Redeem {

  implicit val codec: Codec[Redeem] =
    (implicitly[Codec[PoolId]] ::
      int64 ::
      int64 ::
      implicitly[Codec[RedeemParams]] ::
      implicitly[Codec[Output]]).as[Redeem]
}

@derive(encoder, decoder, loggable)
final case class Swap(poolId: PoolId, maxMinerFee: Long, timestamp: Long, params: SwapParams, box: Output)
  extends CFMMOrder

object Swap {

  implicit val codec: Codec[Swap] =
    (implicitly[Codec[PoolId]] ::
      int64 ::
      int64 ::
      implicitly[Codec[SwapParams]] ::
      implicitly[Codec[Output]]).as[Swap]
}

object CFMMOrder {

  implicit val codec: Codec[CFMMOrder] =
    Codec.coproduct[CFMMOrder].discriminatedByIndex(int8)
}
