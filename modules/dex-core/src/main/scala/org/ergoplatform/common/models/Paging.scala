package org.ergoplatform.common.models

import derevo.derive
import tofu.logging.derivation.loggable

@derive(loggable)
final case class Paging(offset: Int, limit: Int)