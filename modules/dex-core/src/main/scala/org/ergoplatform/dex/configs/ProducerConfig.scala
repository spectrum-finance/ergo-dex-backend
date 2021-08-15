package org.ergoplatform.dex.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.streaming.TopicId
import tofu.logging.derivation.loggable

@derive(pureconfigReader, loggable)
final case class ProducerConfig(topicId: TopicId, parallelism: Int)
