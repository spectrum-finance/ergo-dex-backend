package org.ergoplatform.ergo

import tofu.logging.Loggable

package object domain {
  implicit val regsLoggable: Loggable[Map[RegisterId, SConstant]] =
    Loggable.stringValue.contramap { x =>
      x.toString()
    }
}
