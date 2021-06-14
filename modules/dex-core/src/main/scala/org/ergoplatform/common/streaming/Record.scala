package org.ergoplatform.common.streaming

final case class Record[K, V](key: K, value: V)
