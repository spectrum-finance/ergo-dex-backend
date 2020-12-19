package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.streaming.TopicId

@derive(pureconfigReader)
final case class ProducerConfig(bootstrapServers: List[String], topicId: TopicId)
