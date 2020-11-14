package org.ergoplatform.dex.executor.config

import cats.effect.Sync
import org.ergoplatform.dex.configs.{ConsumerConfig, NetworkConfig, ProtocolConfig}
import org.ergoplatform.dex.streaming.CommitPolicy
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{promote, ClassyOptics}

@ClassyOptics
final case class ConfigBundle(
  @promote commitPolicy: CommitPolicy,
  @promote exchange: ExchangeConfig,
  @promote protocol: ProtocolConfig,
  @promote consumer: ConsumerConfig,
  @promote network: NetworkConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty

  def load[F[_]: Sync](pathOpt: Option[String]): F[ConfigBundle] = ???
}
