package org.ergoplatform.dex.markets.api.v1.models.amm

import cats.syntax.option.none
import enumeratum.{Enum, EnumEntry}
import io.circe.{Decoder, Encoder}
import org.ergoplatform.dex.markets.api.v1.models.amm.ApiMonth.{
  findValues,
  setDate,
  setDatePrevMs,
  withNameInsensitiveEither
}
import cats.syntax.either._

sealed abstract class ApiPool(
  override val entryName: String,
  val poolId: String,
  val poolLp: String,
  val poolText: String
) extends EnumEntry

object ApiPool extends Enum[ApiPool] {
  val values = findValues

  case object ERGSigUSD
    extends ApiPool(
      "ERGSigUSD".toLowerCase,
      "9916d75132593c8b07fe18bd8d583bda1652eed7565cf41a4738ddd90fc992ec",
      "303f39026572bcb4060b51fafc93787a236bb243744babaa99fceb833d61e198",
      "ERG / SigUSD"
    )

  case object ERGSigRSV
    extends ApiPool(
      "ERGSigRSV".toLowerCase,
      "1d5afc59838920bb5ef2a8f9d63825a55b1d48e269d7cecee335d637c3ff5f3f",
      "fa6326a26334f5e933b96470b53b45083374f71912b0d7597f00c2c7ebeb5da6",
      "ERG / SigRSV"
    )

  case object ERGPaideia
    extends ApiPool(
      "ERGPaideia".toLowerCase,
      "666be5df835a48b99c40a395a8aa3ea6ce39ede2cd77c02921d629b9baad8200",
      "879c71d7d9ad213024962824e7f6f225b282dfb818326b46e80e155a11a90544",
      "ERG / Paideia"
    )

  case object ERGergopad
    extends ApiPool(
      "ERGergopad".toLowerCase,
      "d7868533f26db1b1728c1f85c2326a3c0327b57ddab14e41a2b77a5d4c20f4b2",
      "f7cf16e6eed0d11ffd3f55186e00085748e78f487cb6e517b2f610e0045509fe",
      "ERG / ergopad"
    )

  case object ERGNETA
    extends ApiPool(
      "ERGNETA".toLowerCase,
      "7d2e28431063cbb1e9e14468facc47b984d962532c19b0b14f74d0ce9ed459be",
      "e249780a22e14279357103749102d0a7033e0459d10b7f277356522ae9df779c",
      "ERG / NETA"
    )

  case object ERGEGIO
    extends ApiPool(
      "ERGEGIO".toLowerCase,
      "9c1d78e53e7812df96bbb09b757ee1e059c5a298d85789b5c82a7222c34e8f61",
      "660015ebe4666151171d58b4235c8a1a6183cf3e73458e254cc3d14ff9a66ba3",
      "ERG / EGIO"
    )

  case object ERGTerahertz
    extends ApiPool(
      "ERGTerahertz".toLowerCase,
      "0b36eb5086ba1d258341723fa4768acaa3804fba982641a00941d5aad2107f50",
      "88eac61a302e79dfdfea6f15ff4b9a92cfe4252f8ead70dda208447fb542747b",
      "ERG / Terahertz"
    )

  implicit val encoder: Encoder[ApiPool] = Encoder[String].contramap(_.entryName)

  implicit val decoder: Decoder[ApiPool] =
    Decoder[String].emap(s => withNameInsensitiveEither(s).leftMap(_.getMessage()))
}
