package org.ergoplatform.dex.streaming

final case class Record[K, V](key: K, value: V)
