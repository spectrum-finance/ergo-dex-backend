package org.ergoplatform.dex.resolver.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import org.ergoplatform.common.streaming.TopicId

@derive(pureconfigReader)
final case class Topics(limitOrders: TopicId, cfmm: TopicId)
