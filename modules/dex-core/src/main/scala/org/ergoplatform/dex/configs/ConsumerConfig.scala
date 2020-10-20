package org.ergoplatform.dex.configs

import cats.data.NonEmptyList
import org.ergoplatform.dex.streaming.{ClientId, GroupId, TopicId}

final case class ConsumerConfig(
  bootstrapServers: NonEmptyList[String],
  groupId: GroupId,
  clientId: ClientId,
  topicId: TopicId
)
