package org.ergoplatform.dex.configs

import org.ergoplatform.dex.UrlString

final case class ConsumerConfig(
  host: UrlString,
  port: Int,
  groupId: String,
  clientId: String,
  topicId: String
)
