package org.ergoplatform.dex.tracker.context

import cats.effect.Sync
import org.ergoplatform.dex.configs.{ConsumerConfig, ProducerConfig, ProtocolConfig}
import org.ergoplatform.dex.streaming.CommitPolicy
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.{ClassyOptics, promote}

@ClassyOptics
final case class AppContext(
  @promote commitPolicy: CommitPolicy,
  @promote consumerConfig: ConsumerConfig,
  @promote producerConfig: ProducerConfig,
  @promote protocolConfig: ProtocolConfig
)

object AppContext extends Context.Companion[AppContext] {

  implicit val loggable: Loggable[AppContext] = Loggable.empty

  def make[F[_]: Sync](configPathOpt: Option[String]): F[AppContext] = ???
}
