package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.streaming.TopicId

@derive(pureconfigReader)
final case class ProducerConfig(topicId: TopicId, parallelism: Int)
