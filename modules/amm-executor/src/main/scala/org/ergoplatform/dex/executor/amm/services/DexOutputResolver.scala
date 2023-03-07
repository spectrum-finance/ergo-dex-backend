package org.ergoplatform.dex.executor.amm.services

import cats.Monad
import cats.effect.Sync
import cats.effect.concurrent.Ref
import derevo.derive
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.ergo.Address
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import org.ergoplatform.wallet.mnemonic.Mnemonic
import org.ergoplatform.wallet.secrets.ExtendedSecretKey.deriveMasterKey
import org.ergoplatform.wallet.secrets.{DerivationPath, ExtendedSecretKey}
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.lift.IsoK
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

@derive(representableK)
trait DexOutputResolver[F[_]] {
  def getLatest: F[Option[Output]]

  def setPredicted(output: Output): F[Unit]

  def invalidateAndUpdate: F[Unit]
}

object DexOutputResolver {

  def make[I[_]: Sync, F[_]: Sync](
    exchange: ExchangeConfig
  )(implicit
    explorer: ErgoExplorer[F],
    e: ErgoAddressEncoder,
    logs: Logs[I, F],
    iso: IsoK[F, I]
  ): I[DexOutputResolver[F]] =
    logs.forService[DexOutputResolver[F]].flatMap { implicit __ =>
      Ref.in[I, F, Option[Output]](None).flatMap { ref =>
        val seed: Array[Byte] = Mnemonic.toSeed(exchange.mnemonic)
        val SK: ExtendedSecretKey = deriveMasterKey(seed)
        val path = "m/44'/429'/0'/0/0"
        val derivationPath = DerivationPath.fromEncoded(path).get
        val nextSK = SK.derive(derivationPath).asInstanceOf[ExtendedSecretKey]
        val address = Address.fromStringUnsafe(e.toString(P2PKAddress(nextSK.publicImage)))

        iso
          .to(
            explorer
              .getUnspentOutputByAddress(address)
              .flatMap { maybeOutput =>
                ref.set(maybeOutput.map(Output.fromExplorer))
              }
          )
          .map { _ =>
            new Tracing[F] attach new InMemory[F](address, ref): DexOutputResolver[F]
          }
      }
    }

  final private class InMemory[F[_]: Monad](address: Address, cache: Ref[F, Option[Output]])(implicit
    explorer: ErgoExplorer[F]
  ) extends DexOutputResolver[F] {
    def getLatest: F[Option[Output]] = cache.get

    def invalidateAndUpdate: F[Unit] =
      explorer.getUnspentOutputByAddress(address).flatMap { output =>
        output.fold(unit)(output => cache.set(Some(Output.fromExplorer(output))))
      }

    def setPredicted(output: Output): F[Unit] =
      cache.set(Some(output))
  }

  final private class Tracing[F[_]: Monad: Logging] extends DexOutputResolver[Mid[F, *]] {

    def getLatest: Mid[F, Option[Output]] =
      for {
        _ <- info"getLatest()"
        r <- _
        _ <- info"getLatest() -> $r"
      } yield r

    def setPredicted(output: Output): Mid[F, Unit] =
      for {
        _ <- info"setPredicted(${output.boxId})"
        r <- _
        _ <- info"setPredicted() -> success"
      } yield r

    def invalidateAndUpdate: Mid[F, Unit] =
      for {
        _ <- info"invalidateAndUpdate()"
        r <- _
        _ <- info"invalidateAndUpdate() -> success"
      } yield r
  }
}
