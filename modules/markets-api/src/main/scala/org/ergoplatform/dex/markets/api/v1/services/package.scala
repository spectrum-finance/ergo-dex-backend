package org.ergoplatform.dex.markets.api.v1

import java.time.{ZoneId, ZoneOffset}
import java.util.Locale

package object services {
  val Zone: ZoneId    = ZoneId.of("UTC")
  val Utc: ZoneOffset = ZoneOffset.UTC
}
