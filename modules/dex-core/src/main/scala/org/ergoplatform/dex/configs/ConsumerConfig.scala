package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.streaming.{ClientId, GroupId, TopicId}
import tofu.Context

@derive(pureconfigReader)
final case class ConsumerConfig(
  bootstrapServers: List[String],
  groupId: GroupId,
  clientId: ClientId,
  topicId: TopicId
)

object ConsumerConfig extends Context.Companion[ConsumerConfig]
