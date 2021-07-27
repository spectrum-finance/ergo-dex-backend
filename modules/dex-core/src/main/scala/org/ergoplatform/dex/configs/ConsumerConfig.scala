package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.streaming.{ClientId, GroupId, TopicId}
import tofu.Context
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class ConsumerConfig(
  groupId: GroupId,
  clientId: ClientId,
  topicId: TopicId
)

object ConsumerConfig extends Context.Companion[ConsumerConfig]
