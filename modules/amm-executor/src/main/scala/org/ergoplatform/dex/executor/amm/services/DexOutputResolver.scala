package org.ergoplatform.dex.executor.amm.services

import cats.Monad
import cats.effect.Sync
import cats.effect.concurrent.Ref
import derevo.derive
import org.bouncycastle.util.BigIntegers
import org.ergoplatform.{ErgoAddressEncoder, P2PKAddress}
import org.ergoplatform.dex.executor.amm.config.ExchangeConfig
import org.ergoplatform.ergo.Address
import org.ergoplatform.ergo.domain.Output
import org.ergoplatform.ergo.services.explorer.ErgoExplorer
import scorex.util.encode.Base16
import sigmastate.basics.DLogProtocol.DLogProverInput
import tofu.higherKind.Mid
import tofu.higherKind.derived.representableK
import tofu.lift.{IsoK, Lift}
import tofu.logging.{Logging, Logs}
import tofu.syntax.monadic._
import tofu.syntax.logging._
import tofu.syntax.embed._

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
        val sk: DLogProverInput = DLogProverInput(BigIntegers.fromUnsignedByteArray(Base16.decode(exchange.skHex).get))
        val p2pk                = P2PKAddress(sk.publicImage)
        val address             = Address.fromStringUnsafe(e.toString(p2pk))
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
