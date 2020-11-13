package org.ergoplatform.dex.tracker.configs

import cats.effect.Sync
import org.ergoplatform.dex.configs.{ConsumerConfig, ProducerConfig, ProtocolConfig}
import org.ergoplatform.dex.streaming.CommitPolicy
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{ClassyOptics, promote}

@ClassyOptics
final case class ConfigBundle(
  @promote commitPolicy: CommitPolicy,
  @promote consumer: ConsumerConfig,
  @promote producer: ProducerConfig,
  @promote protocol: ProtocolConfig
)

object ConfigBundle extends Context.Companion[ConfigBundle] {

  implicit val loggable: Loggable[ConfigBundle] = Loggable.empty

  def load[F[_]: Sync](pathOpt: Option[String]): F[ConfigBundle] = ???
}
