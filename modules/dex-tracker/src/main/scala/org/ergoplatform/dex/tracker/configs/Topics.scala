package org.ergoplatform.dex.tracker.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.dex.streaming.TopicId

@derive(pureconfigReader)
final case class Topics(limitOrders: TopicId, cfmm: TopicId)
