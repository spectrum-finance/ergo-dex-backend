package org.ergoplatform.dex.watcher.settings

import org.ergoplatform.dex.UrlString

final case class KafkaSettings(
  host: UrlString,
  port: Int,
  groupId: String,
  clientId: String,
  topicId: String
)
